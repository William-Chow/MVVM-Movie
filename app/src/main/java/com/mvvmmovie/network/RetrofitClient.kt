package com.mvvmmovie.network

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit.Builder
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*

object RetrofitClient {

    // Base URL
    private const val BASE_URL = "https://api.themoviedb.org/3/"
    const val API_KEY = "1ee04cdd24bdc8497ec43f739fd3b5a5"

    private val okHttpClient = OkHttpClient()
        .newBuilder()
        .addInterceptor(AuthorizationInterceptor)
        .addInterceptor(RequestInterceptor)
        .build()

    val api : MovieApi by lazy {
        Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MovieApi::class.java)
    }
}

object RequestInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        return chain.proceed(request)
    }
}

object AuthorizationInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val requestWithHeader = chain.request()
            .newBuilder()
            .header(
                "Authorization", UUID.randomUUID().toString()
            ).build()
        return chain.proceed(requestWithHeader)
    }
}