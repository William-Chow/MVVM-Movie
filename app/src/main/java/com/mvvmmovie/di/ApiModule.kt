package com.mvvmmovie.di

import com.mvvmmovie.network.MovieApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Kept apart from [AppModule] so a test can swap the API without rebuilding the whole stack. */
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideMovieApi(retrofit: Retrofit): MovieApi = retrofit.create(MovieApi::class.java)
}
