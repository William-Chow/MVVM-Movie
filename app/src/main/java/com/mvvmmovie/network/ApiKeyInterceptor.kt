package com.mvvmmovie.network

import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Qualifier
import javax.inject.Singleton

/** Marks the TMDB API key, so it is never confused with any other String in the graph. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TmdbApiKey

/** TMDB v3 authenticates through an `api_key` query parameter, so every request gets one here. */
@Singleton
class ApiKeyInterceptor @Inject constructor(
    @param:TmdbApiKey private val apiKey: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url.newBuilder()
            .addQueryParameter(PARAM, apiKey)
            .build()
        return chain.proceed(request.newBuilder().url(url).build())
    }

    companion object {
        private const val PARAM = "api_key"
        private val KEY_IN_URL = Regex("""\bapi_key=[^&\s]+""")

        /** The key travels in the query string, so any URL headed for a log goes through here first. */
        fun redact(message: String): String = message.replace(KEY_IN_URL, "$PARAM=REDACTED")
    }
}
