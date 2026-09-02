package com.mvvmmovie.model

import androidx.annotation.StringRes
import com.mvvmmovie.R

/** A TMDB list endpoint under `movie/`. */
enum class MovieCategory(val path: String, @param:StringRes val labelRes: Int) {
    POPULAR("popular", R.string.category_popular),
    NOW_PLAYING("now_playing", R.string.category_now_playing),
    TOP_RATED("top_rated", R.string.category_top_rated),
    UPCOMING("upcoming", R.string.category_upcoming)
}

/** Where the grid is currently getting its movies from. */
sealed interface MovieSource {

    /** A paged, network-backed list. Only these can be handed to the movies endpoint. */
    sealed interface Remote : MovieSource

    data class Category(val category: MovieCategory) : Remote

    data class Search(val query: String) : Remote

    /** Local only, so it neither pages nor needs the network. */
    data object Favorites : MovieSource

    /** Stable key for the offline cache table. */
    val cacheKey: String
        get() = when (this) {
            is Category -> "category:${category.path}"
            is Search -> "search:$query"
            Favorites -> "favorites"
        }
}

/** Client-side ordering applied to whatever pages are currently loaded. */
enum class MovieSort(@param:StringRes val labelRes: Int) {
    DEFAULT(R.string.sort_default),
    RATING(R.string.sort_rating),
    RELEASE_DATE(R.string.sort_release_date),
    TITLE(R.string.sort_title)
}
