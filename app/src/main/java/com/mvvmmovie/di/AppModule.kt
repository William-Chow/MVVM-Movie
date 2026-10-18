package com.mvvmmovie.di

import android.content.Context
import android.util.Log
import com.mvvmmovie.BuildConfig
import com.mvvmmovie.network.ApiKeyInterceptor
import com.mvvmmovie.network.TmdbApiKey
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val CACHE_BYTES = 10L * 1024 * 1024
    private const val HTTP_LOG_TAG = "OkHttp"

    @Provides
    @Singleton
    @TmdbApiKey
    fun provideApiKey(): String = BuildConfig.TMDB_API_KEY

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        apiKeyInterceptor: ApiKeyInterceptor
    ): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .cache(Cache(context.cacheDir.resolve("http"), CACHE_BYTES))
        .addInterceptor(apiKeyInterceptor)
        .apply {
            if (BuildConfig.DEBUG) {
                val logger = HttpLoggingInterceptor { message ->
                    Log.d(HTTP_LOG_TAG, ApiKeyInterceptor.redact(message))
                }
                addInterceptor(logger.setLevel(HttpLoggingInterceptor.Level.BASIC))
            }
        }
        .build()

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.TMDB_BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

}
