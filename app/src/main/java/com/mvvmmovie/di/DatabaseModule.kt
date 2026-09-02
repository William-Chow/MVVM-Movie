package com.mvvmmovie.di

import android.content.Context
import androidx.room.Room
import com.mvvmmovie.data.local.MovieDao
import com.mvvmmovie.data.local.MovieDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/** Kept apart from [AppModule] so a test can swap in an in-memory database. */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideMovieDatabase(@ApplicationContext context: Context): MovieDatabase =
        Room.databaseBuilder(context, MovieDatabase::class.java, "movies.db").build()

    @Provides
    @Singleton
    fun provideMovieDao(database: MovieDatabase): MovieDao = database.movieDao()
}
