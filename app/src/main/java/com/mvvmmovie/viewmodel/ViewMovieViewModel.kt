package com.mvvmmovie.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.mvvmmovie.model.Movie

class ViewMovieViewModel(savedStateHandle: SavedStateHandle) : ViewModel() {

    /**
     * The Activity's intent extras are seeded into [SavedStateHandle] by the default
     * ViewModel factory, so the movie is read without touching an Intent here and it
     * survives process death.
     */
    val movie: Movie? = savedStateHandle[EXTRA_MOVIE]

    companion object {
        const val EXTRA_MOVIE = "Movie"
    }
}
