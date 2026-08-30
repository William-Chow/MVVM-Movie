package com.mvvmmovie.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.Movies
import com.mvvmmovie.repository.MovieRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class MovieViewModel(
    private val repository: MovieRepository = MovieRepository()
) : ViewModel() {

    private val loadedMovies = mutableListOf<Movie>()
    private var query: String? = null
    private var lastLoadedPage = 0
    private var totalPages = 1
    private var loadJob: Job? = null

    private val uiState = MutableLiveData<MovieUiState>()
    private val loadingMore = MutableLiveData(false)
    private val message = MutableLiveData<String?>()

    init {
        // Loading here instead of from the Activity means a rotation reuses the data
        // already in memory rather than firing the same request again.
        load(page = FIRST_PAGE)
    }

    fun observeUiState(): LiveData<MovieUiState> = uiState

    fun observeLoadingMore(): LiveData<Boolean> = loadingMore

    fun observeMessage(): LiveData<String?> = message

    /** Reloads from page one. Backs both pull-to-refresh and the retry button. */
    fun refresh() = load(page = FIRST_PAGE)

    /** Switches the grid to a search; a null or blank keyword restores the popular list. */
    fun search(keyword: String?) {
        val normalized = keyword?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized == query) return
        query = normalized
        load(page = FIRST_PAGE)
    }

    /** Ignored while a request is in flight or once the last page has been reached. */
    fun loadNextPage() {
        if (loadJob?.isActive == true || lastLoadedPage >= totalPages) return
        load(page = lastLoadedPage + 1)
    }

    fun onMessageShown() {
        message.value = null
    }

    private fun load(page: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            if (page == FIRST_PAGE) {
                uiState.value = MovieUiState.Loading
                loadingMore.value = false
            } else {
                loadingMore.value = true
            }

            val keyword = query
            val result = if (keyword == null) {
                repository.getPopularMovies(page)
            } else {
                repository.searchMovies(keyword, page)
            }

            loadingMore.value = false
            result
                .onSuccess { movies -> onPageLoaded(page, movies) }
                .onFailure { error -> onPageFailed(page, error) }
        }
    }

    private fun onPageLoaded(page: Int, movies: Movies) {
        if (page == FIRST_PAGE) loadedMovies.clear()
        // TMDB can repeat a movie across consecutive pages; keep the grid free of duplicates.
        val knownIds = loadedMovies.mapNotNull { it.id }.toMutableSet()
        loadedMovies += movies.results.orEmpty().filter { it.id == null || knownIds.add(it.id!!) }
        lastLoadedPage = movies.page ?: page
        totalPages = movies.total_pages ?: lastLoadedPage

        uiState.value = if (loadedMovies.isEmpty()) {
            MovieUiState.Empty
        } else {
            MovieUiState.Success(loadedMovies.toList())
        }
    }

    private fun onPageFailed(page: Int, error: Throwable) {
        if (page == FIRST_PAGE) {
            loadedMovies.clear()
            uiState.value = MovieUiState.Error(error.message)
        } else {
            // Keep the pages already on screen; scrolling again retries the next one.
            message.value = error.message
        }
    }

    private companion object {
        const val FIRST_PAGE = 1
    }
}
