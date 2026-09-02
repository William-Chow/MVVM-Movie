package com.mvvmmovie.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.mvvmmovie.model.Movie
import com.mvvmmovie.network.OfflineException
import com.mvvmmovie.repository.MovieRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
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
            uiState.value = MovieDetailUiState.Error(null, offline = false)
            return
        }
        viewModelScope.launch {
            uiState.value = MovieDetailUiState.Loading
            repository.getMovieDetail(movieId)
                .onSuccess { movie ->
                    loadedMovie = movie
                    uiState.value = MovieDetailUiState.Success(movie)
                }
                .onFailure { error ->
                    uiState.value = MovieDetailUiState.Error(error.message, error is OfflineException)
                }
        }
    }

    fun toggleFavorite() {
        val movie = loadedMovie ?: return
        val nowFavorite = isFavorite.value != true
        viewModelScope.launch { repository.setFavorite(movie, nowFavorite) }
    }

    companion object {
        const val EXTRA_MOVIE_ID = "movieId"
        private const val NO_ID = -1
    }
}
