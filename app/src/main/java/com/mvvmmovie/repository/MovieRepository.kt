package com.mvvmmovie.repository

import com.mvvmmovie.model.Movies
import com.mvvmmovie.network.MovieApi
import com.mvvmmovie.network.RetrofitClient
import retrofit2.HttpException
import retrofit2.Response
import java.util.concurrent.CancellationException

/**
 * Single entry point for movie data. The API is injected so tests can hand in a fake
 * instead of hitting the network.
 */
class MovieRepository(private val api: MovieApi = RetrofitClient.api) {

    suspend fun getPopularMovies(page: Int): Result<Movies> = request { api.getPopularMovies(page) }

    suspend fun searchMovies(query: String, page: Int): Result<Movies> =
        request { api.searchMovies(query, page) }

    /** Turns a Retrofit call into a [Result], so an HTTP error is a failure and not an empty body. */
    private suspend fun request(call: suspend () -> Response<Movies>): Result<Movies> = try {
        val response = call()
        val body = response.body()
        if (response.isSuccessful && body != null) {
            Result.success(body)
        } else {
            Result.failure(HttpException(response))
        }
    } catch (cancellation: CancellationException) {
        // Never swallow cancellation: it must keep unwinding the calling coroutine.
        throw cancellation
    } catch (error: Exception) {
        Result.failure(error)
    }
}
