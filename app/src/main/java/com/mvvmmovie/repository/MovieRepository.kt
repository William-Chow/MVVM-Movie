package com.mvvmmovie.repository

import com.mvvmmovie.data.local.CachedMovie
import com.mvvmmovie.data.local.FavoriteMovie
import com.mvvmmovie.data.local.MovieDao
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.MovieSource
import com.mvvmmovie.model.Movies
import com.mvvmmovie.network.MovieApi
import com.mvvmmovie.network.NetworkMonitor
import com.mvvmmovie.network.OfflineException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import retrofit2.Response
import java.util.concurrent.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single entry point for movie data: TMDB for the lists and details, Room for favourites
 * and for the offline copy of each list's first page.
 */
@Singleton
class MovieRepository @Inject constructor(
    private val api: MovieApi,
    private val dao: MovieDao,
    private val networkMonitor: NetworkMonitor
) {

    /** Favourites are local and live-updating, so the type keeps them out of here; see [observeFavorites]. */
    suspend fun getMovies(source: MovieSource.Remote, page: Int): Result<Movies> = when (source) {
        is MovieSource.Category -> request { api.getMovies(source.category.path, page) }
        is MovieSource.Search -> request { api.searchMovies(source.query, page) }
    }

    suspend fun getMovieDetail(id: Int): Result<Movie> = request { api.getMovieDetail(id) }

    // ---- offline copy of the first page ----

    suspend fun cacheFirstPage(source: MovieSource.Remote, movies: List<Movie>) {
        dao.replaceCache(
            source.cacheKey,
            movies.mapIndexedNotNull { index, movie ->
                val id = movie.id ?: return@mapIndexedNotNull null
                CachedMovie(
                    listKey = source.cacheKey,
                    id = id,
                    title = movie.title,
                    posterPath = movie.poster_path,
                    voteAverage = movie.vote_average,
                    releaseDate = movie.release_date,
                    overview = movie.overview,
                    position = index
                )
            }
        )
    }

    suspend fun cachedMovies(source: MovieSource.Remote): List<Movie> =
        dao.cachedMovies(source.cacheKey).map { cached ->
            Movie().apply {
                id = cached.id
                title = cached.title
                poster_path = cached.posterPath
                vote_average = cached.voteAverage
                release_date = cached.releaseDate
                overview = cached.overview
            }
        }

    // ---- favourites ----

    fun observeFavorites(): Flow<List<Movie>> =
        dao.observeFavorites().map { favorites ->
            favorites.map { favorite ->
                Movie().apply {
                    id = favorite.id
                    title = favorite.title
                    poster_path = favorite.posterPath
                    vote_average = favorite.voteAverage
                    release_date = favorite.releaseDate
                    overview = favorite.overview
                }
            }
        }

    fun observeIsFavorite(id: Int): Flow<Boolean> = dao.observeIsFavorite(id)

    suspend fun setFavorite(movie: Movie, favorite: Boolean) {
        val id = movie.id ?: return
        if (favorite) {
            dao.addFavorite(
                FavoriteMovie(
                    id = id,
                    title = movie.title,
                    posterPath = movie.poster_path,
                    voteAverage = movie.vote_average,
                    releaseDate = movie.release_date,
                    overview = movie.overview,
                    addedAt = System.currentTimeMillis()
                )
            )
        } else {
            dao.removeFavorite(id)
        }
    }

    /** Fails fast when offline so callers can say so, instead of leaking a DNS error. */
    private suspend fun <T> request(call: suspend () -> Response<T>): Result<T> = try {
        if (!networkMonitor.isOnline()) {
            Result.failure(OfflineException())
        } else {
            val response = call()
            val body = response.body()
            if (response.isSuccessful && body != null) {
                Result.success(body)
            } else {
                Result.failure(HttpException(response))
            }
        }
    } catch (cancellation: CancellationException) {
        // Never swallow cancellation: it must keep unwinding the calling coroutine.
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
}
