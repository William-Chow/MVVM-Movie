package com.mvvmmovie.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.MovieCategory
import com.mvvmmovie.model.MovieSort
import com.mvvmmovie.model.MovieSource
import com.mvvmmovie.model.Movies
import com.mvvmmovie.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class MovieViewModel @Inject constructor(
    private val repository: MovieRepository,
    private val savedState: SavedStateHandle
) : ViewModel() {

    private val loadedMovies = mutableListOf<Movie>()
    private var lastLoadedPage = 0
    private var totalPages = 1
    private var servedFromCache = false
    private var loadJob: Job? = null
    private var favoritesJob: Job? = null

    // Which list is showing, the list a search replaced and the sort order are kept in
    // SavedStateHandle so they survive process death; the movies themselves are refetched.
    private var source: MovieSource = savedState[KEY_SOURCE] ?: DEFAULT_SOURCE
        set(value) {
            field = value
            savedState[KEY_SOURCE] = value
            sourceState.value = value
        }
    private var sourceBeforeSearch: MovieSource? = savedState[KEY_SOURCE_BEFORE_SEARCH]
        set(value) {
            field = value
            savedState[KEY_SOURCE_BEFORE_SEARCH] = value
        }
    private var sort: MovieSort = savedState[KEY_SORT] ?: MovieSort.DEFAULT
        set(value) {
            field = value
            savedState[KEY_SORT] = value
            sortState.value = value
        }

    private val uiState = MutableLiveData<MovieUiState>()
    private val loadingMore = MutableLiveData(false)
    private val message = MutableLiveData<LoadError?>()
    private val sourceState = MutableLiveData(source)
    private val sortState = MutableLiveData(sort)

    init {
        // Loading here instead of from the Activity means a rotation reuses the data
        // already in memory rather than firing the same request again.
        load(page = FIRST_PAGE)
    }

    fun observeUiState(): LiveData<MovieUiState> = uiState

    fun observeLoadingMore(): LiveData<Boolean> = loadingMore

    fun observeMessage(): LiveData<LoadError?> = message

    fun observeSource(): LiveData<MovieSource> = sourceState

    fun observeSort(): LiveData<MovieSort> = sortState

    /** Reloads from page one. Backs both pull-to-refresh and the retry button. */
    fun refresh() = load(page = FIRST_PAGE)

    fun selectCategory(category: MovieCategory) = showList(MovieSource.Category(category))

    fun showFavorites() = showList(MovieSource.Favorites)

    /**
     * Switches the grid to a search. A null or blank keyword leaves the search and goes back to
     * the list it replaced; with no search showing, it changes nothing.
     */
    fun search(keyword: String?) {
        val normalized = keyword?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized == null) {
            if (source is MovieSource.Search) switchTo(sourceBeforeSearch ?: DEFAULT_SOURCE)
            sourceBeforeSearch = null
            return
        }
        if (source !is MovieSource.Search) sourceBeforeSearch = source
        switchTo(MovieSource.Search(normalized))
    }

    /** Reorders whatever pages are currently loaded; it does not re-query TMDB. */
    fun sortBy(order: MovieSort) {
        if (order == sort) return
        sort = order
        // While a new list loads, or after it failed, the movies in memory are not what the
        // grid shows; the next publish() picks the new order up.
        if (uiState.value is MovieUiState.Success) publish()
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

    /** A list picked from the menu replaces any search, so leaving the search later stays on it. */
    private fun showList(target: MovieSource) {
        sourceBeforeSearch = null
        switchTo(target)
    }

    private fun switchTo(target: MovieSource) {
        if (target == source) return
        source = target
        load(page = FIRST_PAGE)
    }

    private fun load(page: Int) {
        loadJob?.cancel()
        favoritesJob?.cancel()
        // A cancelled page request never reaches the line that hides its spinner.
        loadingMore.value = false

        if (source == MovieSource.Favorites) {
            observeFavorites()
            return
        }

        val remote = source as MovieSource.Remote
        loadJob = viewModelScope.launch {
            if (page == FIRST_PAGE) {
                uiState.value = MovieUiState.Loading
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

    private fun onPageLoaded(source: MovieSource.Remote, page: Int, movies: Movies) {
        if (page == FIRST_PAGE) loadedMovies.clear()
        // TMDB can repeat a movie across consecutive pages; keep the grid free of duplicates.
        val knownIds = loadedMovies.mapNotNull { it.id }.toMutableSet()
        val fresh = movies.results.orEmpty().filter { it.id == null || knownIds.add(it.id!!) }
        loadedMovies += fresh
        lastLoadedPage = movies.page ?: page
        // total_pages can run into the tens of thousands, but TMDB rejects any page past MAX_PAGE.
        totalPages = minOf(movies.total_pages ?: lastLoadedPage, MAX_PAGE)
        servedFromCache = false
        publish()

        if (page == FIRST_PAGE) {
            // Written after the grid is updated so the UI never waits on Room.
            val firstPage = loadedMovies.toList()
            viewModelScope.launch { repository.cacheFirstPage(source, firstPage) }
        }
    }

    private suspend fun onPageFailed(source: MovieSource.Remote, page: Int, error: Throwable) {
        if (page != FIRST_PAGE) {
            // Keep the pages already on screen; scrolling again retries the next one.
            message.value = error.toLoadError()
            return
        }

        // Nothing on screen, so fall back to the last good copy of this list before erroring out.
        // Only a connection problem does: an HTTP or parsing error is reported, not shown as "offline".
        val cached = if (error is IOException) repository.cachedMovies(source) else emptyList()
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
        uiState.value = MovieUiState.Error(error.toLoadError())
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

        /** TMDB's list and search endpoints refuse page numbers above this. */
        const val MAX_PAGE = 500

        val DEFAULT_SOURCE: MovieSource = MovieSource.Category(MovieCategory.POPULAR)

        const val KEY_SOURCE = "source"
        const val KEY_SOURCE_BEFORE_SEARCH = "sourceBeforeSearch"
        const val KEY_SORT = "sort"
    }
}
