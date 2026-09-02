package com.mvvmmovie.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mvvmmovie.data.local.CachedMovie
import com.mvvmmovie.data.local.FavoriteMovie
import com.mvvmmovie.data.local.MovieDao
import com.mvvmmovie.data.local.MovieDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Runs the real Room schema and queries in memory, rather than a hand-written fake. */
@RunWith(AndroidJUnit4::class)
class MovieDaoTest {

    private lateinit var database: MovieDatabase
    private lateinit var dao: MovieDao

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            MovieDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = database.movieDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun favorites_areReturnedNewestFirst() = runTest {
        dao.addFavorite(favorite(1, addedAt = 100))
        dao.addFavorite(favorite(2, addedAt = 300))
        dao.addFavorite(favorite(3, addedAt = 200))

        assertEquals(listOf(2, 3, 1), dao.observeFavorites().first().map { it.id })
    }

    @Test
    fun addFavorite_twiceForTheSameMovie_replacesRatherThanDuplicates() = runTest {
        dao.addFavorite(favorite(1, title = "First"))
        dao.addFavorite(favorite(1, title = "Second"))

        val favorites = dao.observeFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("Second", favorites.single().title)
    }

    @Test
    fun observeIsFavorite_tracksAddAndRemove() = runTest {
        assertFalse(dao.observeIsFavorite(7).first())

        dao.addFavorite(favorite(7))
        assertTrue(dao.observeIsFavorite(7).first())

        dao.removeFavorite(7)
        assertFalse(dao.observeIsFavorite(7).first())
    }

    @Test
    fun removeFavorite_leavesOtherMoviesAlone() = runTest {
        dao.addFavorite(favorite(1))
        dao.addFavorite(favorite(2))

        dao.removeFavorite(1)

        assertEquals(listOf(2), dao.observeFavorites().first().map { it.id })
    }

    @Test
    fun cachedMovies_comeBackInTheStoredPositionOrder() = runTest {
        dao.insertCache(
            listOf(
                cached("popular", id = 10, position = 2),
                cached("popular", id = 11, position = 0),
                cached("popular", id = 12, position = 1)
            )
        )

        assertEquals(listOf(11, 12, 10), dao.cachedMovies("popular").map { it.id })
    }

    @Test
    fun replaceCache_swapsOneListWithoutTouchingAnother() = runTest {
        dao.insertCache(listOf(cached("popular", id = 1, position = 0)))
        dao.insertCache(listOf(cached("top_rated", id = 2, position = 0)))

        dao.replaceCache("popular", listOf(cached("popular", id = 3, position = 0)))

        assertEquals(listOf(3), dao.cachedMovies("popular").map { it.id })
        assertEquals(listOf(2), dao.cachedMovies("top_rated").map { it.id })
    }

    @Test
    fun sameMovieCanBeCachedUnderTwoLists() = runTest {
        dao.insertCache(
            listOf(
                cached("popular", id = 5, position = 0),
                cached("upcoming", id = 5, position = 0)
            )
        )

        assertEquals(listOf(5), dao.cachedMovies("popular").map { it.id })
        assertEquals(listOf(5), dao.cachedMovies("upcoming").map { it.id })
    }

    @Test
    fun cachedMovies_forAnUnknownList_isEmpty() = runTest {
        assertTrue(dao.cachedMovies("nothing-here").isEmpty())
    }

    private fun favorite(id: Int, title: String = "Movie $id", addedAt: Long = id.toLong()) =
        FavoriteMovie(
            id = id,
            title = title,
            posterPath = "/poster$id.jpg",
            voteAverage = 7.5,
            releaseDate = "2024-01-0$id",
            overview = "Overview $id",
            addedAt = addedAt
        )

    private fun cached(listKey: String, id: Int, position: Int) = CachedMovie(
        listKey = listKey,
        id = id,
        title = "Movie $id",
        posterPath = "/poster$id.jpg",
        voteAverage = 6.0,
        releaseDate = "2024-02-01",
        overview = "Overview $id",
        position = position
    )
}
