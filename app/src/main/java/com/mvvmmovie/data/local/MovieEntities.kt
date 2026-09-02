package com.mvvmmovie.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_movies")
data class FavoriteMovie(
    @PrimaryKey val id: Int,
    val title: String?,
    val posterPath: String?,
    val voteAverage: Double?,
    val releaseDate: String?,
    val overview: String?,
    val addedAt: Long
)

/**
 * Last successfully loaded first page per list, so a cold start with no network still
 * shows something instead of an error page.
 */
@Entity(tableName = "cached_movies", primaryKeys = ["listKey", "id"])
data class CachedMovie(
    val listKey: String,
    val id: Int,
    val title: String?,
    val posterPath: String?,
    val voteAverage: Double?,
    val releaseDate: String?,
    val overview: String?,
    val position: Int
)
