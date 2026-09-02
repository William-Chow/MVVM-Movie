package com.mvvmmovie.view

import android.content.Intent
import android.os.Looper
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mvvmmovie.R
import com.mvvmmovie.data.local.MovieDao
import com.mvvmmovie.data.local.MovieDatabase
import com.mvvmmovie.di.ApiModule
import com.mvvmmovie.di.DatabaseModule
import com.mvvmmovie.di.NetworkModule
import com.mvvmmovie.fake.FakeMovieApi
import com.mvvmmovie.fake.FakeNetworkMonitor
import com.mvvmmovie.model.Cast
import com.mvvmmovie.model.Credits
import com.mvvmmovie.model.Genre
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.Movies
import com.mvvmmovie.model.Video
import com.mvvmmovie.model.Videos
import com.mvvmmovie.network.MovieApi
import com.mvvmmovie.network.NetworkMonitor
import com.mvvmmovie.viewmodel.ViewMovieViewModel
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import dagger.hilt.android.testing.UninstallModules
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import retrofit2.Response
import java.io.IOException

@HiltAndroidTest
@UninstallModules(ApiModule::class, NetworkModule::class, DatabaseModule::class)
@Config(application = HiltTestApplication::class)
@RunWith(AndroidJUnit4::class)
class ViewMovieActivityTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @BindValue
    @JvmField
    val networkMonitor: NetworkMonitor = FakeNetworkMonitor()

    @BindValue
    @JvmField
    var api: MovieApi = FakeMovieApi(detail = { Response.success(fullMovie()) })

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

    @Test
    fun detail_rendersEveryFieldTheApiReturned() {
        launch().use { scenario ->
            settle()
            scenario.onActivity { activity ->
                assertEquals("The Matrix", activity.text(R.id.tvTitle))
                assertEquals("Free your mind", activity.text(R.id.tvTagline))
                assertTrue(activity.text(R.id.tvMeta).contains("2h 16m"))
                assertEquals("Action  ·  Science Fiction", activity.text(R.id.tvGenres))
                assertTrue(activity.text(R.id.tvFinancials).contains("$63,000,000"))
                assertEquals("Warner Bros.", activity.text(R.id.tvStudios))
            }
        }
    }

    @Test
    fun detail_showsCastTrailersAndSimilarSections() {
        launch().use { scenario ->
            settle()
            scenario.onActivity { activity ->
                assertEquals(1, activity.recycler(R.id.rvCast).adapter?.itemCount)
                assertEquals(1, activity.recycler(R.id.rvTrailers).adapter?.itemCount)
                assertEquals(1, activity.recycler(R.id.rvSimilar).adapter?.itemCount)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.tvCastLabel).visibility)
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.tvSimilarLabel).visibility)
            }
        }
    }

    @Test
    fun trailersWithoutAYouTubeKey_areLeftOut() {
        api = FakeMovieApi(detail = {
            Response.success(fullMovie().apply {
                videos = Videos().apply {
                    results = listOf(video(site = "Vimeo"), video(site = "YouTube"))
                }
            })
        })

        launch().use { scenario ->
            settle()
            scenario.onActivity { activity ->
                assertEquals(1, activity.recycler(R.id.rvTrailers).adapter?.itemCount)
            }
        }
    }

    @Test
    fun favouriteToggle_writesThroughToTheDatabase() = runTest {
        launch().use { scenario ->
            settle()
            scenario.onActivity { activity ->
                activity.findViewById<View>(R.id.action_favorite)?.performClick()
                    ?: activity.viewModelToggle()
            }
            settle()
        }

        assertEquals(listOf(603), dao.observeFavorites().first().map { it.id })
    }

    @Test
    fun failure_showsTheErrorContainer() {
        api = FakeMovieApi(detail = { throw IOException("boom") })

        launch().use { scenario ->
            settle()
            scenario.onActivity { activity ->
                assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.errorContainer).visibility)
                assertEquals("boom", activity.text(R.id.tvError))
                assertEquals(View.GONE, activity.findViewById<View>(R.id.content).visibility)
            }
        }
    }

    @Test
    fun offline_showsTheNoNetworkMessage() {
        (networkMonitor as FakeNetworkMonitor).online = false

        launch().use { scenario ->
            settle()
            scenario.onActivity { activity ->
                assertEquals(
                    activity.getString(R.string.error_no_network),
                    activity.text(R.id.tvError)
                )
            }
        }
    }

    private fun launch(): ActivityScenario<ViewMovieActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ViewMovieActivity::class.java)
            .putExtra(ViewMovieViewModel.EXTRA_MOVIE_ID, 603)
        return ActivityScenario.launch(intent)
    }

    private fun settle() = Shadows.shadowOf(Looper.getMainLooper()).idle()

    private fun ViewMovieActivity.text(id: Int) = findViewById<TextView>(id).text.toString()

    private fun ViewMovieActivity.recycler(id: Int) = findViewById<RecyclerView>(id)

    /** The star lives in the options menu, which Robolectric does not attach as a view. */
    private fun ViewMovieActivity.viewModelToggle() {
        val provider = androidx.lifecycle.ViewModelProvider(this)
        provider[ViewMovieViewModel::class.java].toggleFavorite()
    }

    private companion object {

        fun video(site: String) = Video().apply {
            id = "v-$site"
            key = "key-$site"
            name = "Trailer"
            this.site = site
        }

        fun fullMovie() = Movie().apply {
            id = 603
            title = "The Matrix"
            tagline = "Free your mind"
            overview = "A hacker learns the truth."
            runtime = 136
            budget = 63_000_000
            revenue = 463_000_000
            vote_average = 8.2
            release_date = "1999-03-30"
            genres = listOf(
                Genre().apply { id = 28; name = "Action" },
                Genre().apply { id = 878; name = "Science Fiction" }
            )
            production_companies = listOf(
                com.mvvmmovie.model.ProductionCompanies().apply { id = 1; name = "Warner Bros." }
            )
            credits = Credits().apply {
                cast = listOf(Cast().apply { id = 6384; name = "Keanu Reeves"; character = "Neo" })
            }
            videos = Videos().apply { results = listOf(video(site = "YouTube")) }
            similar = Movies().apply {
                page = 1
                total_pages = 1
                results = listOf(Movie().apply { id = 604; title = "Reloaded" })
            }
        }
    }
}
