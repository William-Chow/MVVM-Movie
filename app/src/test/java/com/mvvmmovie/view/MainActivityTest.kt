package com.mvvmmovie.view

import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mvvmmovie.R
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.mvvmmovie.data.local.MovieDao
import com.mvvmmovie.data.local.MovieDatabase
import com.mvvmmovie.di.ApiModule
import com.mvvmmovie.di.DatabaseModule
import com.mvvmmovie.di.NetworkModule
import com.mvvmmovie.fake.FakeMovieApi
import com.mvvmmovie.fake.FakeNetworkMonitor
import com.mvvmmovie.fake.movie
import com.mvvmmovie.fake.page
import com.mvvmmovie.network.MovieApi
import com.mvvmmovie.network.NetworkMonitor
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.IOException

/** Drives the real Activity with a fake API behind Hilt, so the view wiring is covered too. */
@HiltAndroidTest
@UninstallModules(ApiModule::class, NetworkModule::class, DatabaseModule::class)
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    @JvmField
    val networkMonitor: NetworkMonitor = FakeNetworkMonitor()

    @BindValue
    @JvmField
    var api: MovieApi = FakeMovieApi()

    /**
     * Direct executors, so a DAO call finishes inline instead of leaving the coroutine
     * parked on Room's background pool while the test asserts.
     */
    private val database: MovieDatabase = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        MovieDatabase::class.java
    ).setQueryExecutor { it.run() }
        .setTransactionExecutor { it.run() }
        .allowMainThreadQueries()
        .build()

    @BindValue
    @JvmField
    val dao: MovieDao = database.movieDao()

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @After
    fun tearDown() {
        database.close()
    }

    /** Lets the posted LiveData and coroutine work run before anything is asserted. */
    private fun settle() {
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }

    @Test
    fun grid_rendersTheMoviesReturnedByTheApi() {
        api = FakeMovieApi(movies = { _, p -> page(p, totalPages = 1, movie(1, title = "Dune"), movie(2, title = "Arrival")) })

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { activity ->
                val recycler = activity.findViewById<RecyclerView>(R.id.rvMovies)
                assertEquals(2, recycler.adapter?.itemCount)
                assertEquals("Dune", firstItemTitle(recycler))
            }
        }
    }

    @Test
    fun failure_showsTheErrorContainerWithRetry() {
        api = FakeMovieApi(movies = { _, _ -> throw IOException("boom") })

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { activity ->
                assertEquals(android.view.View.VISIBLE, activity.findViewById<android.view.View>(R.id.errorContainer).visibility)
                assertEquals("boom", activity.findViewById<TextView>(R.id.tvError).text.toString())
            }
        }
    }

    @Test
    fun retry_asksTheApiAgain() {
        val fake = FakeMovieApi(movies = { _, _ -> throw IOException("boom") })
        api = fake

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { activity ->
                assertEquals(1, fake.requestedCategories.size)
                activity.findViewById<android.view.View>(R.id.btnRetry).performClick()
                assertEquals(2, fake.requestedCategories.size)
            }
        }
    }

    @Test
    fun offline_showsTheNoNetworkMessageRatherThanARawError() {
        (networkMonitor as FakeNetworkMonitor).online = false

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { activity ->
                assertEquals(
                    activity.getString(R.string.error_no_network),
                    activity.findViewById<TextView>(R.id.tvError).text.toString()
                )
            }
        }
    }

    @Test
    fun emptyResult_showsTheEmptyLabel() {
        api = FakeMovieApi(movies = { _, p -> page(p, totalPages = 1) })

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { activity ->
                val empty = activity.findViewById<TextView>(R.id.tvEmpty)
                assertEquals(android.view.View.VISIBLE, empty.visibility)
                assertEquals(activity.getString(R.string.empty_result), empty.text.toString())
            }
        }
    }

    @Test
    fun tappingAMovie_startsTheDetailScreenWithItsId() {
        api = FakeMovieApi(movies = { _, p -> page(p, totalPages = 1, movie(603, title = "The Matrix")) })

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            settle()
            scenario.onActivity { activity ->
                val recycler = activity.findViewById<RecyclerView>(R.id.rvMovies)
                measure(recycler)
                recycler.findViewHolderForAdapterPosition(0)!!.itemView.performClick()

                val started = org.robolectric.Shadows.shadowOf(activity).nextStartedActivity
                assertEquals(
                    ViewMovieActivity::class.java.name,
                    started.component?.className
                )
                assertEquals(603, started.getIntExtra("movieId", -1))
            }
        }
    }

    private fun firstItemTitle(recycler: RecyclerView): String {
        measure(recycler)
        val holder = recycler.findViewHolderForAdapterPosition(0)!!
        return holder.itemView.findViewById<TextView>(R.id.tvMovieName).text.toString()
    }

    /** Robolectric does not lay out automatically, so the grid needs a size before it binds. */
    private fun measure(recycler: RecyclerView) {
        recycler.measure(
            android.view.View.MeasureSpec.makeMeasureSpec(1080, android.view.View.MeasureSpec.EXACTLY),
            android.view.View.MeasureSpec.makeMeasureSpec(1920, android.view.View.MeasureSpec.EXACTLY)
        )
        recycler.layout(0, 0, 1080, 1920)
    }
}
