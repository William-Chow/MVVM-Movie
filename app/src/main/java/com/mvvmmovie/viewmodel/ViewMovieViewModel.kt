package com.mvvmmovie.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mvvmmovie.model.Movie
import com.mvvmmovie.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ViewMovieViewModel @Inject constructor(
    private val repository: MovieRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /**
     * The Activity's intent extras are seeded into [SavedStateHandle] by the default
     * ViewModel factory, so the id is read without touching an Intent here and it
     * survives process death.
     */
    private val movieId: Int = savedStateHandle[EXTRA_MOVIE_ID] ?: NO_ID

    private val uiState = MutableLiveData<MovieDetailUiState>()

    /** Live, so favouriting from this screen updates the star without a reload. */
    val isFavorite: LiveData<Boolean> = repository.observeIsFavorite(movieId).asLiveData()

    private var loadedMovie: Movie? = null

    init {
        load()
    }

    fun observeUiState(): LiveData<MovieDetailUiState> = uiState

    fun load() {
        if (movieId == NO_ID) {
            uiState.value = MovieDetailUiState.Error(LoadError.UNKNOWN)
            return
        }
        viewModelScope.launch {
            uiState.value = MovieDetailUiState.Loading
            repository.getMovieDetail(movieId)
                .onSuccess { movie -> show(movie, fromSavedCopy = false) }
                .onFailure { error ->
                    // Without a connection, fall back to what the grid or the favourites kept, so
                    // a movie already on the device still opens (and can be unfavourited) offline.
                    val saved = if (error is IOException) repository.savedMovie(movieId) else null
                    if (saved != null) {
                        show(saved, fromSavedCopy = true)
                    } else {
                        uiState.value = MovieDetailUiState.Error(error.toLoadError())
                    }
                }
        }
    }

    fun toggleFavorite() {
        val movie = loadedMovie ?: return
        val nowFavorite = isFavorite.value != true
        viewModelScope.launch { repository.setFavorite(movie, nowFavorite) }
    }

    private fun show(movie: Movie, fromSavedCopy: Boolean) {
        loadedMovie = movie
        uiState.value = MovieDetailUiState.Success(movie, fromSavedCopy)
    }

    companion object {
        const val EXTRA_MOVIE_ID = "movieId"
        private const val NO_ID = -1
    }
}
