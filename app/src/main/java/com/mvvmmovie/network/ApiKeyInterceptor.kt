package com.mvvmmovie.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/** TMDB v3 authenticates through an `api_key` query parameter, so every request gets one here. */
@Singleton
class ApiKeyInterceptor @Inject constructor(
    private val apiKey: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.newBuilder()
            .addQueryParameter("api_key", apiKey)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}
