package com.mvvmmovie.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.Movies
import com.mvvmmovie.network.MovieApi
import com.mvvmmovie.repository.MovieRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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

/**
 * Drives [MovieViewModel] against a fake [MovieApi]. The unconfined test dispatcher runs
 * viewModelScope work eagerly, so every state is settled by the time an assertion runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MovieViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

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
        val api = FakeMovieApi(popular = { page -> page(page, totalPages = 2, movie(1), movie(2)) })

        val viewModel = createViewModel(api)

        assertEquals(listOf(1), api.popularPages)
        assertEquals(listOf(1, 2), viewModel.movieIds())
    }

    @Test
    fun noResults_showsEmptyState() {
        val api = FakeMovieApi(popular = { page -> page(page, totalPages = 1) })

        val viewModel = createViewModel(api)

        assertEquals(MovieUiState.Empty, viewModel.observeUiState().value)
    }

    @Test
    fun httpError_showsErrorStateInsteadOfEmptyGrid() {
        val api = FakeMovieApi(popular = { httpError(401) })

        val viewModel = createViewModel(api)

        assertTrue(viewModel.observeUiState().value is MovieUiState.Error)
    }

    @Test
    fun networkFailure_showsErrorState() {
        val api = FakeMovieApi(popular = { throw IOException("offline") })

        val viewModel = createViewModel(api)

        val state = viewModel.observeUiState().value
        assertTrue(state is MovieUiState.Error)
        assertEquals("offline", (state as MovieUiState.Error).message)
    }

    @Test
    fun loadNextPage_appendsToTheMoviesAlreadyOnScreen() {
        val api = FakeMovieApi(popular = { page ->
            when (page) {
                1 -> page(1, totalPages = 2, movie(1), movie(2))
                else -> page(2, totalPages = 2, movie(3))
            }
        })
        val viewModel = createViewModel(api)

        viewModel.loadNextPage()

        assertEquals(listOf(1, 2), api.popularPages)
        assertEquals(listOf(1, 2, 3), viewModel.movieIds())
    }

    @Test
    fun loadNextPage_isIgnoredOnTheLastPage() {
        val api = FakeMovieApi(popular = { page -> page(page, totalPages = 1, movie(1)) })
        val viewModel = createViewModel(api)

        viewModel.loadNextPage()

        assertEquals(listOf(1), api.popularPages)
    }

    @Test
    fun loadNextPage_dropsMoviesRepeatedFromAnEarlierPage() {
        val api = FakeMovieApi(popular = { page ->
            when (page) {
                1 -> page(1, totalPages = 2, movie(1), movie(2))
                else -> page(2, totalPages = 2, movie(2), movie(3))
            }
        })
        val viewModel = createViewModel(api)

        viewModel.loadNextPage()

        assertEquals(listOf(1, 2, 3), viewModel.movieIds())
    }

    @Test
    fun laterPageFailure_keepsTheGridAndReportsTheMessage() {
        val api = FakeMovieApi(popular = { page ->
            if (page == 1) page(1, totalPages = 3, movie(1)) else throw IOException("offline")
        })
        val viewModel = createViewModel(api)

        viewModel.loadNextPage()

        assertEquals(listOf(1), viewModel.movieIds())
        assertEquals("offline", viewModel.observeMessage().value)

        viewModel.onMessageShown()
        assertNull(viewModel.observeMessage().value)
    }

    @Test
    fun search_queriesTheSearchEndpointFromTheFirstPage() {
        val api = FakeMovieApi(
            popular = { page -> page(page, totalPages = 5, movie(1)) },
            search = { _, page -> page(page, totalPages = 1, movie(9)) }
        )
        val viewModel = createViewModel(api)

        viewModel.search("dune")

        assertEquals(listOf("dune" to 1), api.searchCalls)
        assertEquals(listOf(9), viewModel.movieIds())
    }

    @Test
    fun search_repeatedWithTheSameKeyword_doesNotRefetch() {
        val api = FakeMovieApi(
            popular = { page -> page(page, totalPages = 5, movie(1)) },
            search = { _, page -> page(page, totalPages = 1, movie(9)) }
        )
        val viewModel = createViewModel(api)

        viewModel.search("dune")
        viewModel.search("  dune  ")

        assertEquals(1, api.searchCalls.size)
    }

    @Test
    fun search_clearedWithBlankKeyword_restoresThePopularList() {
        val api = FakeMovieApi(
            popular = { page -> page(page, totalPages = 5, movie(1)) },
            search = { _, page -> page(page, totalPages = 1, movie(9)) }
        )
        val viewModel = createViewModel(api)
        viewModel.search("dune")

        viewModel.search("")

        assertEquals(listOf(1, 1), api.popularPages)
        assertEquals(listOf(1), viewModel.movieIds())
    }

    @Test
    fun refresh_reloadsFromTheFirstPage() {
        val api = FakeMovieApi(popular = { page -> page(page, totalPages = 3, movie(1)) })
        val viewModel = createViewModel(api)
        viewModel.loadNextPage()

        viewModel.refresh()

        assertEquals(listOf(1, 2, 1), api.popularPages)
        assertEquals(listOf(1), viewModel.movieIds())
    }

    private fun createViewModel(api: MovieApi) = MovieViewModel(MovieRepository(api))

    private fun MovieViewModel.movieIds(): List<Int?> {
        val state = observeUiState().value
        assertTrue("expected Success but was $state", state is MovieUiState.Success)
        return (state as MovieUiState.Success).movies.map { it.id }
    }

    private class FakeMovieApi(
        private val popular: (Int) -> Response<Movies> = { page(it, totalPages = 1) },
        private val search: (String, Int) -> Response<Movies> = { _, page -> page(page, totalPages = 1) }
    ) : MovieApi {

        val popularPages = mutableListOf<Int>()
        val searchCalls = mutableListOf<Pair<String, Int>>()

        override suspend fun getPopularMovies(page: Int): Response<Movies> {
            popularPages += page
            return popular(page)
        }

        override suspend fun searchMovies(query: String, page: Int): Response<Movies> {
            searchCalls += query to page
            return search(query, page)
        }
    }

    private companion object {

        fun movie(id: Int) = Movie().apply {
            this.id = id
            this.title = "Movie $id"
        }

        fun page(page: Int, totalPages: Int, vararg results: Movie): Response<Movies> =
            Response.success(Movies().apply {
                this.page = page
                this.total_pages = totalPages
                this.results = results.toList()
            })

        fun httpError(code: Int): Response<Movies> =
            Response.error(code, "".toResponseBody("application/json".toMediaType()))
    }
}
