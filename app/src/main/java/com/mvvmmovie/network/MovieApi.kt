package com.mvvmmovie.network

import com.mvvmmovie.model.Movies
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface MovieApi {

    @GET("movie/popular")
    suspend fun getPopularMovies(@Query("page") page: Int): Response<Movies>

    @GET("search/movie")
    suspend fun searchMovies(
        @Query("query") query: String,
        @Query("page") page: Int
    ): Response<Movies>
}
