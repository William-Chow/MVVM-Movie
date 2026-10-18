package com.mvvmmovie.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieDao {

    @Query("SELECT * FROM favorite_movies ORDER BY addedAt DESC")
    fun observeFavorites(): Flow<List<FavoriteMovie>>

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_movies WHERE id = :id)")
    fun observeIsFavorite(id: Int): Flow<Boolean>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(movie: FavoriteMovie)

    @Query("DELETE FROM favorite_movies WHERE id = :id")
    suspend fun removeFavorite(id: Int)

    @Query("SELECT * FROM favorite_movies WHERE id = :id")
    suspend fun favorite(id: Int): FavoriteMovie?

    @Query("SELECT * FROM cached_movies WHERE listKey = :listKey ORDER BY position")
    suspend fun cachedMovies(listKey: String): List<CachedMovie>

    /** Any list's copy of the movie; they only differ in which list they came from. */
    @Query("SELECT * FROM cached_movies WHERE id = :id LIMIT 1")
    suspend fun cachedMovie(id: Int): CachedMovie?

    @Query("DELETE FROM cached_movies WHERE listKey = :listKey")
    suspend fun clearCache(listKey: String)

    @Query("DELETE FROM cached_movies WHERE listKey LIKE :prefix || '%'")
    suspend fun clearCachesWithPrefix(prefix: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(movies: List<CachedMovie>)

    @Transaction
    suspend fun replaceCache(listKey: String, movies: List<CachedMovie>) {
        clearCache(listKey)
        insertCache(movies)
    }

    /** Leaves exactly one list under [prefix]: the one in [movies]. */
    @Transaction
    suspend fun replaceCachesWithPrefix(prefix: String, movies: List<CachedMovie>) {
        clearCachesWithPrefix(prefix)
        insertCache(movies)
    }
}
