package com.mvvmmovie.network

import com.mvvmmovie.model.Movies
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface MovieApi {
    @GET("movie/popular?")
    fun getPopularMovies(@Query("api_key") api_key : String) : Call<Movies>
}