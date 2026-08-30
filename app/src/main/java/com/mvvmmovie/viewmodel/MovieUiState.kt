package com.mvvmmovie.viewmodel

import com.mvvmmovie.model.Movie

/** What the movie grid should be showing right now. */
sealed interface MovieUiState {

    /** First page is on its way and there is nothing to show yet. */
    data object Loading : MovieUiState

    /** The request came back with no movies at all (typically a search that matched nothing). */
    data object Empty : MovieUiState

    /** Movies to render, already including every page loaded so far. */
    data class Success(val movies: List<Movie>) : MovieUiState

    /** The first page failed and the grid is blank; the user can retry. */
    data class Error(val message: String?) : MovieUiState
}
