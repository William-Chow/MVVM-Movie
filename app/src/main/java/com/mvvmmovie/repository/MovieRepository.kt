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

    /**
     * The movie as the device last stored it, a favourite first and then any cached list
     * entry, so the detail screen has something to show offline. Null when neither has it.
     */
    suspend fun savedMovie(id: Int): Movie? =
        dao.favorite(id)?.toMovie() ?: dao.cachedMovie(id)?.toMovie()

    // ---- offline copy of the first page ----

    /**
     * Best effort: a failed write is dropped rather than taking the freshly loaded list down
     * with it. Each category keeps its own copy; searches share one slot holding the latest,
     * so ad-hoc queries cannot grow the table without bound.
     */
    suspend fun cacheFirstPage(source: MovieSource.Remote, movies: List<Movie>) {
        val rows = movies.mapIndexedNotNull { index, movie ->
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
        try {
            when (source) {
                is MovieSource.Category -> dao.replaceCache(source.cacheKey, rows)
                is MovieSource.Search -> dao.replaceCachesWithPrefix(MovieSource.Search.CACHE_KEY_PREFIX, rows)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            // The cache is only a fallback; the next successful load writes it again.
        }
    }

    suspend fun cachedMovies(source: MovieSource.Remote): List<Movie> =
        dao.cachedMovies(source.cacheKey).map { it.toMovie() }

    // ---- favourites ----

    fun observeFavorites(): Flow<List<Movie>> =
        dao.observeFavorites().map { favorites -> favorites.map { it.toMovie() } }

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

    private fun FavoriteMovie.toMovie() = Movie().also { movie ->
        movie.id = id
        movie.title = title
        movie.poster_path = posterPath
        movie.vote_average = voteAverage
        movie.release_date = releaseDate
        movie.overview = overview
    }

    private fun CachedMovie.toMovie() = Movie().also { movie ->
        movie.id = id
        movie.title = title
        movie.poster_path = posterPath
        movie.vote_average = voteAverage
        movie.release_date = releaseDate
        movie.overview = overview
    }
}
