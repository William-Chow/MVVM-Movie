package com.mvvmmovie.fake

import com.mvvmmovie.data.local.CachedMovie
import com.mvvmmovie.data.local.FavoriteMovie
import com.mvvmmovie.data.local.MovieDao
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.Movies
import com.mvvmmovie.network.MovieApi
import com.mvvmmovie.network.NetworkMonitor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import retrofit2.Response

class FakeNetworkMonitor(var online: Boolean = true) : NetworkMonitor {
    override fun isOnline() = online
}

/** In-memory stand-in for Room, so DAO-backed behaviour is testable without a device. */
class FakeMovieDao : MovieDao {

    private val favorites = MutableStateFlow<List<FavoriteMovie>>(emptyList())
    private val cache = mutableMapOf<String, List<CachedMovie>>()

    /** Makes every cache write throw, the way a full disk would. */
    var failCacheWrites = false

    /** Every list key that currently holds a cached copy. */
    val cachedListKeys: Set<String> get() = cache.keys.toSet()

    override fun observeFavorites(): Flow<List<FavoriteMovie>> = favorites

    override fun observeIsFavorite(id: Int): Flow<Boolean> =
        favorites.map { list -> list.any { it.id == id } }

    override suspend fun addFavorite(movie: FavoriteMovie) {
        favorites.value = favorites.value.filterNot { it.id == movie.id } + movie
    }

    override suspend fun removeFavorite(id: Int) {
        favorites.value = favorites.value.filterNot { it.id == id }
    }

    override suspend fun favorite(id: Int): FavoriteMovie? = favorites.value.firstOrNull { it.id == id }

    override suspend fun cachedMovies(listKey: String): List<CachedMovie> =
        cache[listKey].orEmpty()

    override suspend fun cachedMovie(id: Int): CachedMovie? =
        cache.values.flatten().firstOrNull { it.id == id }

    override suspend fun clearCache(listKey: String) {
        checkWritable()
        cache.remove(listKey)
    }

    override suspend fun clearCachesWithPrefix(prefix: String) {
        checkWritable()
        cache.keys.removeAll { it.startsWith(prefix) }
    }

    override suspend fun insertCache(movies: List<CachedMovie>) {
        checkWritable()
        movies.groupBy { it.listKey }.forEach { (key, value) ->
            cache[key] = cache[key].orEmpty() + value
        }
    }

    private fun checkWritable() = check(!failCacheWrites) { "database or disk is full" }
}

/** Each endpoint is a suspend function, so a test can hold a response back with a CompletableDeferred. */
class FakeMovieApi(
    private val movies: suspend (String, Int) -> Response<Movies> = { _, page -> page(page, totalPages = 1) },
    private val search: suspend (String, Int) -> Response<Movies> = { _, page -> page(page, totalPages = 1) },
    private val detail: suspend (Int) -> Response<Movie> = { Response.success(movie(it)) }
) : MovieApi {

    val requestedCategories = mutableListOf<Pair<String, Int>>()
    val searchCalls = mutableListOf<Pair<String, Int>>()
    val detailCalls = mutableListOf<Int>()

    override suspend fun getMovies(category: String, page: Int): Response<Movies> {
        requestedCategories += category to page
        return movies(category, page)
    }

    override suspend fun searchMovies(query: String, page: Int): Response<Movies> {
        searchCalls += query to page
        return search(query, page)
    }

    override suspend fun getMovieDetail(id: Int, append: String): Response<Movie> {
        detailCalls += id
        return detail(id)
    }
}

fun movie(id: Int, rating: Double = 0.0, released: String = "", title: String = "Movie $id") =
    Movie().apply {
        this.id = id
        this.title = title
        this.vote_average = rating
        this.release_date = released
    }

fun page(page: Int, totalPages: Int, vararg results: Movie): Response<Movies> =
    Response.success(Movies().apply {
        this.page = page
        this.total_pages = totalPages
        this.results = results.toList()
    })
