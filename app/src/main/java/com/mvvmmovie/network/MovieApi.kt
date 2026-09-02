package com.mvvmmovie.network

import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.Movies
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MovieApi {

    /** [category] is a TMDB list name: popular, now_playing, top_rated or upcoming. */
    @GET("movie/{category}")
    suspend fun getMovies(
        @Path("category") category: String,
        @Query("page") page: Int
    ): Response<Movies>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int
    ): Response<Movies>

    /**
     * Cast, trailers and similar titles ride along on the detail call through
     * append_to_response, so opening a movie costs one request instead of four.
     */
    @GET("movie/{id}")
    suspend fun getMovieDetail(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "credits,videos,similar"
    ): Response<Movie>
}
