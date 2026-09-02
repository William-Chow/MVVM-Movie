package com.mvvmmovie.repository

import com.mvvmmovie.fake.FakeMovieDao
import com.mvvmmovie.fake.FakeNetworkMonitor
import com.mvvmmovie.fake.movie
import com.mvvmmovie.model.MovieCategory
import com.mvvmmovie.model.MovieSource
import com.mvvmmovie.network.ApiKeyInterceptor
import com.mvvmmovie.network.MovieApi
import com.mvvmmovie.network.OfflineException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Exercises the repository over a real Retrofit/OkHttp/Gson stack against MockWebServer, so
 * the interceptor, the endpoint paths and the JSON mapping are all covered.
 */
class MovieRepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: MovieApi

    private val dao = FakeMovieDao()
    private val networkMonitor = FakeNetworkMonitor()

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient.Builder().addInterceptor(ApiKeyInterceptor(API_KEY)).build())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MovieApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getMovies_hitsTheCategoryPathAndAppendsTheApiKey() = runTest {
        server.enqueue(jsonResponse(POPULAR_JSON))

        val result = repository().getMovies(MovieSource.Category(MovieCategory.TOP_RATED), page = 2)

        val request = server.takeRequest()
        assertEquals("/movie/top_rated?page=2&api_key=$API_KEY", request.path)
        assertEquals(listOf(603, 604), result.getOrThrow().results?.map { it.id })
    }

    @Test
    fun searchMovies_hitsTheSearchPath() = runTest {
        server.enqueue(jsonResponse(POPULAR_JSON))

        repository().getMovies(MovieSource.Search("dune"), page = 1)

        assertEquals("/search/movie?query=dune&page=1&api_key=$API_KEY", server.takeRequest().path)
    }

    @Test
    fun getMovieDetail_asksForCreditsVideosAndSimilar() = runTest {
        server.enqueue(jsonResponse(DETAIL_JSON))

        val movie = repository().getMovieDetail(603).getOrThrow()

        val path = server.takeRequest().path.orEmpty()
        assertTrue(path.startsWith("/movie/603?"))
        assertTrue(path.contains("append_to_response=credits%2Cvideos%2Csimilar"))
        assertEquals(listOf("Action"), movie.genres?.map { it.name })
        assertEquals(listOf("Keanu Reeves"), movie.credits?.cast?.map { it.name })
        assertEquals(listOf("abc123"), movie.videos?.results?.map { it.key })
        assertEquals(listOf(604), movie.similar?.results?.map { it.id })
    }

    @Test
    fun httpError_becomesAFailureRatherThanAnEmptyBody() = runTest {
        server.enqueue(MockResponse().setResponseCode(401).setBody("{}"))

        val result = repository().getMovies(MovieSource.Category(MovieCategory.POPULAR), page = 1)

        assertTrue(result.exceptionOrNull() is HttpException)
    }

    @Test
    fun offline_failsBeforeReachingTheNetwork() = runTest {
        networkMonitor.online = false

        val result = repository().getMovies(MovieSource.Category(MovieCategory.POPULAR), page = 1)

        assertTrue(result.exceptionOrNull() is OfflineException)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun cacheFirstPage_roundTripsThroughTheDao() = runTest {
        val repository = repository()
        val source = MovieSource.Category(MovieCategory.UPCOMING)

        repository.cacheFirstPage(source, listOf(movie(1), movie(2)))

        assertEquals(listOf(1, 2), repository.cachedMovies(source).map { it.id })
        // A different list keeps its own copy.
        assertTrue(repository.cachedMovies(MovieSource.Category(MovieCategory.POPULAR)).isEmpty())
    }

    @Test
    fun cacheFirstPage_replacesTheEarlierCopyOfTheSameList() = runTest {
        val repository = repository()
        val source = MovieSource.Category(MovieCategory.POPULAR)

        repository.cacheFirstPage(source, listOf(movie(1), movie(2)))
        repository.cacheFirstPage(source, listOf(movie(3)))

        assertEquals(listOf(3), repository.cachedMovies(source).map { it.id })
    }

    @Test
    fun setFavorite_addsAndRemoves() = runTest {
        val repository = repository()

        repository.setFavorite(movie(7, title = "Heat"), favorite = true)
        assertEquals(listOf(7), repository.observeFavorites().first().map { it.id })
        assertTrue(repository.observeIsFavorite(7).first())

        repository.setFavorite(movie(7), favorite = false)
        assertTrue(repository.observeFavorites().first().isEmpty())
    }

    private fun repository() = MovieRepository(api, dao, networkMonitor)

    private fun jsonResponse(body: String) = MockResponse().setBody(body)

    private companion object {
        const val API_KEY = "test-key"

        val POPULAR_JSON = """
            {"page":1,"total_pages":3,"total_results":2,
             "results":[{"id":603,"title":"The Matrix"},{"id":604,"title":"The Matrix Reloaded"}]}
        """.trimIndent()

        val DETAIL_JSON = """
            {"id":603,"title":"The Matrix","runtime":136,"tagline":"Free your mind",
             "genres":[{"id":28,"name":"Action"}],
             "credits":{"cast":[{"id":6384,"name":"Keanu Reeves","character":"Neo"}]},
             "videos":{"results":[{"id":"v1","key":"abc123","name":"Trailer","site":"YouTube"}]},
             "similar":{"page":1,"total_pages":1,"results":[{"id":604,"title":"Reloaded"}]}}
        """.trimIndent()
    }
}
