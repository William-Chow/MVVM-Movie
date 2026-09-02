package com.mvvmmovie.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.MovieCategory
import com.mvvmmovie.model.MovieSort
import com.mvvmmovie.model.MovieSource
import com.mvvmmovie.model.Movies
import com.mvvmmovie.network.OfflineException
import com.mvvmmovie.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: MovieRepository
) : ViewModel() {

    private val loadedMovies = mutableListOf<Movie>()
    private var lastLoadedPage = 0
    private var totalPages = 1
    private var servedFromCache = false
    private var loadJob: Job? = null
    private var favoritesJob: Job? = null

    private val uiState = MutableLiveData<MovieUiState>()
    private val loadingMore = MutableLiveData(false)
    private val message = MutableLiveData<String?>()
    private val sourceState = MutableLiveData<MovieSource>()
    private val sortState = MutableLiveData(MovieSort.DEFAULT)

    private var source: MovieSource = MovieSource.Category(MovieCategory.POPULAR)
        set(value) {
            field = value
            sourceState.value = value
        }
    private var sort: MovieSort = MovieSort.DEFAULT

    init {
        // Loading here instead of from the Activity means a rotation reuses the data
        // already in memory rather than firing the same request again.
        sourceState.value = source
        load(page = FIRST_PAGE)
    }

    fun observeUiState(): LiveData<MovieUiState> = uiState

    fun observeLoadingMore(): LiveData<Boolean> = loadingMore

    fun observeMessage(): LiveData<String?> = message

    fun observeSource(): LiveData<MovieSource> = sourceState

    fun observeSort(): LiveData<MovieSort> = sortState

    /** Reloads from page one. Backs both pull-to-refresh and the retry button. */
    fun refresh() = load(page = FIRST_PAGE)

    fun selectCategory(category: MovieCategory) = switchTo(MovieSource.Category(category))

    fun showFavorites() = switchTo(MovieSource.Favorites)

    /** Switches the grid to a search; a null or blank keyword restores the popular list. */
    fun search(keyword: String?) {
        val normalized = keyword?.trim()?.takeIf { it.isNotEmpty() }
        val target = if (normalized == null) {
            MovieSource.Category(MovieCategory.POPULAR)
        } else {
            MovieSource.Search(normalized)
        }
        switchTo(target)
    }

    /** Reorders whatever pages are currently loaded; it does not re-query TMDB. */
    fun sortBy(order: MovieSort) {
        if (order == sort) return
        sort = order
        sortState.value = order
        publish()
    }

    /** Ignored while a request is in flight, on the last page, or for the local favourites list. */
    fun loadNextPage() {
        if (source == MovieSource.Favorites || servedFromCache) return
        if (loadJob?.isActive == true || lastLoadedPage >= totalPages) return
        load(page = lastLoadedPage + 1)
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun switchTo(target: MovieSource) {
        if (target == source) return
        source = target
        load(page = FIRST_PAGE)
    }

    private fun load(page: Int) {
        loadJob?.cancel()
        favoritesJob?.cancel()

        if (source == MovieSource.Favorites) {
            observeFavorites()
            return
        }

        val remote = source as MovieSource.Remote
        loadJob = viewModelScope.launch {
            if (page == FIRST_PAGE) {
                uiState.value = MovieUiState.Loading
                loadingMore.value = false
            } else {
                loadingMore.value = true
            }

            val result = repository.getMovies(remote, page)

            loadingMore.value = false
            result
                .onSuccess { movies -> onPageLoaded(remote, page, movies) }
                .onFailure { error -> onPageFailed(remote, page, error) }
        }
    }

    /** Favourites live in Room, so the grid follows the table instead of polling it. */
    private fun observeFavorites() {
        favoritesJob = viewModelScope.launch {
            uiState.value = MovieUiState.Loading
            repository.observeFavorites().collectLatest { favorites ->
                loadedMovies.clear()
                loadedMovies += favorites
                lastLoadedPage = FIRST_PAGE
                totalPages = FIRST_PAGE
                servedFromCache = false
                publish()
            }
        }
    }

    private suspend fun onPageLoaded(source: MovieSource.Remote, page: Int, movies: Movies) {
        if (page == FIRST_PAGE) loadedMovies.clear()
        // TMDB can repeat a movie across consecutive pages; keep the grid free of duplicates.
        val knownIds = loadedMovies.mapNotNull { it.id }.toMutableSet()
        val fresh = movies.results.orEmpty().filter { it.id == null || knownIds.add(it.id!!) }
        loadedMovies += fresh
        lastLoadedPage = movies.page ?: page
        totalPages = movies.total_pages ?: lastLoadedPage
        servedFromCache = false

        if (page == FIRST_PAGE) repository.cacheFirstPage(source, loadedMovies.toList())
        publish()
    }

    private suspend fun onPageFailed(source: MovieSource.Remote, page: Int, error: Throwable) {
        if (page != FIRST_PAGE) {
            // Keep the pages already on screen; scrolling again retries the next one.
            message.value = error.message
            return
        }

        // Nothing on screen, so fall back to the last good copy of this list before erroring out.
        val cached = repository.cachedMovies(source)
        if (cached.isNotEmpty()) {
            loadedMovies.clear()
            loadedMovies += cached
            servedFromCache = true
            lastLoadedPage = FIRST_PAGE
            totalPages = FIRST_PAGE
            publish()
            return
        }

        loadedMovies.clear()
        uiState.value = MovieUiState.Error(error.message, error is OfflineException)
    }

    private fun publish() {
        uiState.value = if (loadedMovies.isEmpty()) {
            MovieUiState.Empty
        } else {
            MovieUiState.Success(sorted(loadedMovies), servedFromCache)
        }
    }

    private fun sorted(movies: List<Movie>): List<Movie> = when (sort) {
        MovieSort.DEFAULT -> movies.toList()
        MovieSort.RATING -> movies.sortedByDescending { it.vote_average ?: 0.0 }
        MovieSort.RELEASE_DATE -> movies.sortedByDescending { it.release_date.orEmpty() }
        MovieSort.TITLE -> movies.sortedBy { it.title.orEmpty().lowercase() }
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
