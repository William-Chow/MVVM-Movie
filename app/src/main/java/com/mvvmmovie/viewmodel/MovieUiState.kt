package com.mvvmmovie.viewmodel

import com.mvvmmovie.model.Movie

/** What the movie grid should be showing right now. */
sealed interface MovieUiState {

    /** First page is on its way and there is nothing to show yet. */
    data object Loading : MovieUiState

    /** The request came back with no movies at all (an empty search, or no favourites yet). */
    data object Empty : MovieUiState

    /**
     * Movies to render, already including every page loaded so far.
     * [fromCache] marks a list served from Room because the network was unreachable.
     */
    data class Success(val movies: List<Movie>, val fromCache: Boolean = false) : MovieUiState

    /** The first page failed and the grid is blank; the user can retry. */
    data class Error(val message: String?, val offline: Boolean) : MovieUiState
}

/** What the detail screen should be showing right now. */
sealed interface MovieDetailUiState {

    data object Loading : MovieDetailUiState

    data class Success(val movie: Movie) : MovieDetailUiState

    data class Error(val message: String?, val offline: Boolean) : MovieDetailUiState
}
