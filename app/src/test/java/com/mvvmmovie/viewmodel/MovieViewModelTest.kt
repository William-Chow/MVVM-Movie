package com.mvvmmovie.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import com.mvvmmovie.fake.FakeMovieApi
import com.mvvmmovie.fake.FakeMovieDao
import com.mvvmmovie.fake.FakeNetworkMonitor
import com.mvvmmovie.fake.movie
import com.mvvmmovie.fake.page
import com.mvvmmovie.model.MovieCategory
import com.mvvmmovie.model.MovieSort
import com.mvvmmovie.model.MovieSource
import com.mvvmmovie.model.Movies
import com.mvvmmovie.repository.MovieRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

/**
 * Drives [MovieViewModel] against fake collaborators. The unconfined test dispatcher runs
 * viewModelScope work eagerly, so every state is settled by the time an assertion runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovieViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val dao = FakeMovieDao()
    private val networkMonitor = FakeNetworkMonitor()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun firstPage_isLoadedOnCreation() {
        val api = FakeMovieApi(movies = { _, page -> page(page, totalPages = 2, movie(1), movie(2)) })

        val viewModel = createViewModel(api)

        assertEquals(listOf("popular" to 1), api.requestedCategories)
        assertEquals(listOf(1, 2), viewModel.movieIds())
    }

    @Test
    fun noResults_showsEmptyState() {
        val api = FakeMovieApi(movies = { _, page -> page(page, totalPages = 1) })

        val viewModel = createViewModel(api)

        assertEquals(MovieUiState.Empty, viewModel.observeUiState().value)
    }

    @Test
    fun httpError_showsErrorStateInsteadOfEmptyGrid() {
        val api = FakeMovieApi(movies = { _, _ -> httpError(401) })

        val viewModel = createViewModel(api)

        val state = viewModel.observeUiState().value
        assertEquals(MovieUiState.Error(LoadError.AUTH), state)
    }

    @Test
    fun offline_withNoCache_showsOfflineError() {
        networkMonitor.online = false

        val viewModel = createViewModel(FakeMovieApi())

        val state = viewModel.observeUiState().value
        assertEquals(MovieUiState.Error(LoadError.OFFLINE), state)
    }

    @Test
    fun offline_withCachedFirstPage_fallsBackToTheCache() = runTest {
        val api = FakeMovieApi(movies = { _, page -> page(page, totalPages = 2, movie(1), movie(2)) })
        createViewModel(api)

        // Same repository, so the cache written by the first load is still there.
        networkMonitor.online = false
        val offlineViewModel = createViewModel(api)

        val state = offlineViewModel.observeUiState().value
        assertTrue("expected cached Success but was $state", state is MovieUiState.Success)
        assertTrue((state as MovieUiState.Success).fromCache)
        assertEquals(listOf(1, 2), state.movies.map { it.id })
    }

    @Test
    fun networkFailure_showsErrorState() {
        val api = FakeMovieApi(movies = { _, _ -> throw IOException("boom") })

        val viewModel = createViewModel(api)

        val state = viewModel.observeUiState().value
        assertEquals(MovieUiState.Error(LoadError.NETWORK), state)
    }

    @Test
    fun loadNextPage_appendsToTheMoviesAlreadyOnScreen() {
        val api = FakeMovieApi(movies = { _, page ->
            if (page == 1) page(1, totalPages = 2, movie(1), movie(2)) else page(2, totalPages = 2, movie(3))
        })
        val viewModel = createViewModel(api)

        viewModel.loadNextPage()

        assertEquals(listOf("popular" to 1, "popular" to 2), api.requestedCategories)
        assertEquals(listOf(1, 2, 3), viewModel.movieIds())
    }

    @Test
    fun loadNextPage_isIgnoredOnTheLastPage() {
        val api = FakeMovieApi(movies = { _, page -> page(page, totalPages = 1, movie(1)) })
        val viewModel = createViewModel(api)

        viewModel.loadNextPage()

        assertEquals(1, api.requestedCategories.size)
    }

    @Test
    fun loadNextPage_dropsMoviesRepeatedFromAnEarlierPage() {
        val api = FakeMovieApi(movies = { _, page ->
            if (page == 1) page(1, totalPages = 2, movie(1), movie(2)) else page(2, totalPages = 2, movie(2), movie(3))
        })
        val viewModel = createViewModel(api)

        viewModel.loadNextPage()

        assertEquals(listOf(1, 2, 3), viewModel.movieIds())
    }

    @Test
    fun laterPageFailure_keepsTheGridAndReportsTheMessage() {
        val api = FakeMovieApi(movies = { _, page ->
            if (page == 1) page(1, totalPages = 3, movie(1)) else throw IOException("boom")
        })
        val viewModel = createViewModel(api)

        viewModel.loadNextPage()

        assertEquals(listOf(1), viewModel.movieIds())
        assertEquals(LoadError.NETWORK, viewModel.observeMessage().value)

        viewModel.onMessageShown()
        assertNull(viewModel.observeMessage().value)
    }

    @Test
    fun selectCategory_switchesTheEndpointAndRestartsFromPageOne() {
        val api = FakeMovieApi(movies = { _, page -> page(page, totalPages = 5, movie(1)) })
        val viewModel = createViewModel(api)
        viewModel.loadNextPage()

        viewModel.selectCategory(MovieCategory.TOP_RATED)

        assertEquals(
            listOf("popular" to 1, "popular" to 2, "top_rated" to 1),
            api.requestedCategories
        )
        assertEquals(
            MovieSource.Category(MovieCategory.TOP_RATED),
            viewModel.observeSource().value
        )
    }

    @Test
    fun search_queriesTheSearchEndpointFromTheFirstPage() {
        val api = FakeMovieApi(search = { _, page -> page(page, totalPages = 1, movie(9)) })
        val viewModel = createViewModel(api)

        viewModel.search("dune")

        assertEquals(listOf("dune" to 1), api.searchCalls)
        assertEquals(listOf(9), viewModel.movieIds())
    }

    @Test
    fun search_repeatedWithTheSameKeyword_doesNotRefetch() {
        val api = FakeMovieApi(search = { _, page -> page(page, totalPages = 1, movie(9)) })
        val viewModel = createViewModel(api)

        viewModel.search("dune")
        viewModel.search("  dune  ")

        assertEquals(1, api.searchCalls.size)
    }

    @Test
    fun search_clearedWithBlankKeyword_restoresThePopularList() {
        val api = FakeMovieApi(
            movies = { _, page -> page(page, totalPages = 5, movie(1)) },
            search = { _, page -> page(page, totalPages = 1, movie(9)) }
        )
        val viewModel = createViewModel(api)
        viewModel.search("dune")

        viewModel.search("")

        assertEquals(listOf("popular" to 1, "popular" to 1), api.requestedCategories)
        assertEquals(listOf(1), viewModel.movieIds())
    }

    @Test
    fun sortBy_reordersTheLoadedPagesWithoutRefetching() {
        val api = FakeMovieApi(movies = { _, page ->
            page(page, totalPages = 1, movie(1, rating = 5.0), movie(2, rating = 9.0), movie(3, rating = 7.0))
        })
        val viewModel = createViewModel(api)

        viewModel.sortBy(MovieSort.RATING)

        assertEquals(listOf(2, 3, 1), viewModel.movieIds())
        assertEquals(1, api.requestedCategories.size)
    }

    @Test
    fun sortByTitle_ordersAlphabetically() {
        val api = FakeMovieApi(movies = { _, page ->
            page(page, totalPages = 1, movie(1, title = "Zodiac"), movie(2, title = "Arrival"))
        })
        val viewModel = createViewModel(api)

        viewModel.sortBy(MovieSort.TITLE)

        assertEquals(listOf(2, 1), viewModel.movieIds())
    }

    @Test
    fun showFavorites_readsFromRoomAndFollowsLaterChanges() = runTest {
        val api = FakeMovieApi(movies = { _, page -> page(page, totalPages = 1, movie(1)) })
        val repository = createRepository(api)
        val viewModel = MovieViewModel(repository, SavedStateHandle())

        viewModel.showFavorites()
        assertEquals(MovieUiState.Empty, viewModel.observeUiState().value)

        repository.setFavorite(movie(42), favorite = true)
        assertEquals(listOf(42), viewModel.movieIds())

        repository.setFavorite(movie(42), favorite = false)
        assertEquals(MovieUiState.Empty, viewModel.observeUiState().value)
    }

    @Test
    fun favorites_doNotTriggerNetworkPaging() {
        val api = FakeMovieApi(movies = { _, page -> page(page, totalPages = 9, movie(1)) })
        val viewModel = createViewModel(api)

        viewModel.showFavorites()
        viewModel.loadNextPage()

        assertEquals(listOf("popular" to 1), api.requestedCategories)
    }

    @Test
    fun serverError_isReportedAsATmdbProblem() {
        val viewModel = createViewModel(FakeMovieApi(movies = { _, _ -> httpError(503) }))

        assertEquals(MovieUiState.Error(LoadError.SERVER), viewModel.observeUiState().value)
    }

    @Test
    fun httpError_withACachedFirstPage_isReportedRatherThanShownAsOffline() {
        var failing = false
        val api = FakeMovieApi(movies = { _, page ->
            if (failing) httpError(401) else page(page, totalPages = 1, movie(1))
        })
        createViewModel(api)

        failing = true
        val viewModel = createViewModel(api)

        assertEquals(MovieUiState.Error(LoadError.AUTH), viewModel.observeUiState().value)
    }

    @Test
    fun timeout_withACachedFirstPage_fallsBackToTheCache() {
        var failing = false
        val api = FakeMovieApi(movies = { _, page ->
            if (failing) throw SocketTimeoutException("timeout") else page(page, totalPages = 1, movie(1))
        })
        createViewModel(api)

        failing = true
        val state = createViewModel(api).observeUiState().value

        assertTrue("expected cached Success but was $state", state is MovieUiState.Success)
        assertTrue((state as MovieUiState.Success).fromCache)
    }

    @Test
    fun failedCacheWrite_stillShowsTheLoadedList() {
        dao.failCacheWrites = true

        val viewModel = createViewModel(FakeMovieApi(movies = { _, page -> page(page, totalPages = 1, movie(1)) }))

        assertEquals(listOf(1), viewModel.movieIds())
    }

    @Test
    fun sortBy_onTheErrorScreen_keepsTheErrorAndItsRetry() {
        val viewModel = createViewModel(FakeMovieApi(movies = { _, _ -> throw IOException("boom") }))

        viewModel.sortBy(MovieSort.RATING)

        assertEquals(MovieUiState.Error(LoadError.NETWORK), viewModel.observeUiState().value)
    }

    @Test
    fun sortBy_whileANewListLoads_doesNotBringThePreviousListBack() {
        val topRated = CompletableDeferred<Response<Movies>>()
        val api = FakeMovieApi(movies = { category, page ->
            if (category == "top_rated") topRated.await() else page(page, totalPages = 1, movie(1), movie(2))
        })
        val viewModel = createViewModel(api)
        viewModel.selectCategory(MovieCategory.TOP_RATED)

        viewModel.sortBy(MovieSort.RATING)
        assertEquals(MovieUiState.Loading, viewModel.observeUiState().value)

        topRated.complete(page(1, totalPages = 1, movie(7, rating = 5.0), movie(8, rating = 9.0)))
        assertEquals(listOf(8, 7), viewModel.movieIds())
    }

    @Test
    fun switchingToFavorites_whileAPageLoads_hidesTheLoadingMoreSpinner() {
        val api = FakeMovieApi(movies = { _, page ->
            if (page == 1) page(1, totalPages = 3, movie(1)) else CompletableDeferred<Response<Movies>>().await()
        })
        val viewModel = createViewModel(api)
        viewModel.loadNextPage()
        assertEquals(true, viewModel.observeLoadingMore().value)

        viewModel.showFavorites()

        assertEquals(false, viewModel.observeLoadingMore().value)
    }

    @Test
    fun loadNextPage_stopsAtTheLastPageTmdbServes() {
        val api = FakeMovieApi(movies = { _, page -> page(page, totalPages = 40_000, movie(page)) })
        val viewModel = createViewModel(api)

        repeat(600) { viewModel.loadNextPage() }

        assertEquals(500, api.requestedCategories.size)
        assertEquals("popular" to 500, api.requestedCategories.last())
    }

    @Test
    fun search_closedWithoutSearching_keepsTheCurrentList() {
        val api = FakeMovieApi()
        val viewModel = createViewModel(api)
        viewModel.showFavorites()

        viewModel.search(null)

        assertEquals(MovieSource.Favorites, viewModel.observeSource().value)
        assertEquals(listOf("popular" to 1), api.requestedCategories)
    }

    @Test
    fun search_cleared_returnsToTheListTheSearchReplaced() {
        val viewModel = createViewModel(FakeMovieApi())
        viewModel.selectCategory(MovieCategory.TOP_RATED)
        viewModel.search("dune")
        viewModel.search("arrival")

        viewModel.search(null)

        assertEquals(MovieSource.Category(MovieCategory.TOP_RATED), viewModel.observeSource().value)
    }

    @Test
    fun listPickedDuringASearch_isKeptWhenTheSearchCloses() {
        val viewModel = createViewModel(FakeMovieApi())
        viewModel.showFavorites()
        viewModel.search("dune")
        viewModel.selectCategory(MovieCategory.UPCOMING)

        viewModel.search(null)

        assertEquals(MovieSource.Category(MovieCategory.UPCOMING), viewModel.observeSource().value)
    }

    @Test
    fun listSearchAndSort_surviveProcessDeath() {
        val api = FakeMovieApi(search = { _, page -> page(page, totalPages = 1, movie(9)) })
        val savedState = SavedStateHandle()
        createViewModel(api, savedState).apply {
            showFavorites()
            search("dune")
            sortBy(MovieSort.TITLE)
        }

        // After process death the system builds a fresh ViewModel over the restored handle.
        val restored = createViewModel(api, savedState)

        assertEquals(MovieSource.Search("dune"), restored.observeSource().value)
        assertEquals(MovieSort.TITLE, restored.observeSort().value)
        assertEquals(listOf("dune" to 1, "dune" to 1), api.searchCalls)

        restored.search(null)
        assertEquals(MovieSource.Favorites, restored.observeSource().value)
    }

    private fun createRepository(api: FakeMovieApi) = MovieRepository(api, dao, networkMonitor)

    private fun createViewModel(api: FakeMovieApi, savedState: SavedStateHandle = SavedStateHandle()) =
        MovieViewModel(createRepository(api), savedState)

    private fun MovieViewModel.movieIds(): List<Int?> {
        val state = observeUiState().value
        assertTrue("expected Success but was $state", state is MovieUiState.Success)
        return (state as MovieUiState.Success).movies.map { it.id }
    }

    private fun httpError(code: Int): Response<com.mvvmmovie.model.Movies> =
        Response.error(code, "".toResponseBody("application/json".toMediaType()))
}
