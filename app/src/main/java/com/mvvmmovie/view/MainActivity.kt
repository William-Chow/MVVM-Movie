package com.mvvmmovie.view

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mvvmmovie.R
import com.mvvmmovie.adapter.MovieAdapter
import com.mvvmmovie.databinding.ActivityMainBinding
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.MovieCategory
import com.mvvmmovie.model.MovieSort
import com.mvvmmovie.model.MovieSource
import com.mvvmmovie.viewmodel.MovieUiState
import com.mvvmmovie.viewmodel.MovieViewModel
import com.mvvmmovie.viewmodel.ViewMovieViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var movieAdapter: MovieAdapter

    private val viewModel: MovieViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()

        prepareRecyclerView()
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.btnRetry.setOnClickListener { viewModel.refresh() }
        addMenuProvider(menuProvider, this@MainActivity)

        observeViewModel()
    }

    private fun prepareRecyclerView() {
        movieAdapter = MovieAdapter { movie -> openMovie(movie) }
        binding.rvMovies.apply {
            layoutManager = GridLayoutManager(this@MainActivity, SPAN_COUNT)
            adapter = movieAdapter
            addOnScrollListener(paginationListener)
        }
    }

    private fun observeViewModel() {
        viewModel.observeUiState().observe(this@MainActivity) { state ->
            // The pull-to-refresh spinner already covers the "reloading" case, so the
            // centred progress bar is only for the very first load.
            val pullRefreshing = binding.swipeRefresh.isRefreshing
            binding.progressBar.isVisible = state is MovieUiState.Loading && !pullRefreshing
            binding.errorContainer.isVisible = state is MovieUiState.Error
            binding.tvEmpty.isVisible = state is MovieUiState.Empty
            binding.tvOfflineBanner.isVisible = state is MovieUiState.Success && state.fromCache

            when (state) {
                is MovieUiState.Success -> movieAdapter.submitList(state.movies)
                is MovieUiState.Empty -> {
                    binding.tvEmpty.setText(
                        if (viewModel.observeSource().value == MovieSource.Favorites) {
                            R.string.empty_favorites
                        } else {
                            R.string.empty_result
                        }
                    )
                    movieAdapter.submitList(emptyList())
                }
                is MovieUiState.Error -> {
                    binding.tvError.text = when {
                        state.offline -> getString(R.string.error_no_network)
                        else -> state.message ?: getString(R.string.error_unknown)
                    }
                    movieAdapter.submitList(emptyList())
                }
                MovieUiState.Loading -> Unit
            }
            if (state !is MovieUiState.Loading) binding.swipeRefresh.isRefreshing = false
        }

        viewModel.observeLoadingMore().observe(this@MainActivity) { loadingMore ->
            binding.progressBarMore.isVisible = loadingMore
        }

        viewModel.observeMessage().observe(this@MainActivity) { message ->
            if (message == null) return@observe
            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }

        viewModel.observeSource().observe(this@MainActivity) { source ->
            title = when (source) {
                is MovieSource.Category -> getString(source.category.labelRes)
                is MovieSource.Search -> source.query
                MovieSource.Favorites -> getString(R.string.action_favorites)
            }
            invalidateOptionsMenu()
        }

        viewModel.observeSort().observe(this@MainActivity) { invalidateOptionsMenu() }
    }

    /** Navigation belongs to the view, not to the ViewModel. */
    private fun openMovie(movie: Movie) {
        val id = movie.id ?: return
        val intent = Intent(this@MainActivity, ViewMovieActivity::class.java)
        intent.putExtra(ViewMovieViewModel.EXTRA_MOVIE_ID, id)
        startActivity(intent)
    }

    private val paginationListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            if (dy <= 0) return
            val layoutManager = recyclerView.layoutManager as GridLayoutManager
            if (layoutManager.findLastVisibleItemPosition() >= layoutManager.itemCount - LOAD_MORE_THRESHOLD) {
                viewModel.loadNextPage()
            }
        }
    }

    private val menuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_main, menu)
            val searchItem = menu.findItem(R.id.action_search)
            (searchItem.actionView as SearchView).apply {
                queryHint = getString(R.string.search_hint)
                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        viewModel.search(query)
                        clearFocus()
                        return true
                    }

                    override fun onQueryTextChange(newText: String?) = false
                })
            }
            searchItem.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
                override fun onMenuItemActionExpand(item: MenuItem) = true

                override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                    viewModel.search(null)
                    return true
                }
            })
        }

        override fun onPrepareMenu(menu: Menu) {
            val source = viewModel.observeSource().value
            val checkedList = when (source) {
                is MovieSource.Category -> categoryMenuId(source.category)
                MovieSource.Favorites -> R.id.action_favorites
                else -> null
            }
            checkedList?.let { menu.findItem(it)?.isChecked = true }
            menu.findItem(sortMenuId(viewModel.observeSort().value ?: MovieSort.DEFAULT))?.isChecked = true
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean = when (menuItem.itemId) {
            R.id.category_popular -> selectCategory(MovieCategory.POPULAR)
            R.id.category_now_playing -> selectCategory(MovieCategory.NOW_PLAYING)
            R.id.category_top_rated -> selectCategory(MovieCategory.TOP_RATED)
            R.id.category_upcoming -> selectCategory(MovieCategory.UPCOMING)
            R.id.action_favorites -> { viewModel.showFavorites(); true }
            R.id.sort_default -> applySort(MovieSort.DEFAULT)
            R.id.sort_rating -> applySort(MovieSort.RATING)
            R.id.sort_release_date -> applySort(MovieSort.RELEASE_DATE)
            R.id.sort_title -> applySort(MovieSort.TITLE)
            else -> false
        }
    }

    private fun selectCategory(category: MovieCategory): Boolean {
        viewModel.selectCategory(category)
        binding.rvMovies.scrollToPosition(0)
        return true
    }

    private fun applySort(order: MovieSort): Boolean {
        viewModel.sortBy(order)
        binding.rvMovies.scrollToPosition(0)
        return true
    }

    private fun categoryMenuId(category: MovieCategory) = when (category) {
        MovieCategory.POPULAR -> R.id.category_popular
        MovieCategory.NOW_PLAYING -> R.id.category_now_playing
        MovieCategory.TOP_RATED -> R.id.category_top_rated
        MovieCategory.UPCOMING -> R.id.category_upcoming
    }

    private fun sortMenuId(order: MovieSort) = when (order) {
        MovieSort.DEFAULT -> R.id.sort_default
        MovieSort.RATING -> R.id.sort_rating
        MovieSort.RELEASE_DATE -> R.id.sort_release_date
        MovieSort.TITLE -> R.id.sort_title
    }

    /** targetSdk 36 draws edge to edge, so the content has to keep clear of the system bars itself. */
    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            windowInsets
        }
    }

    private companion object {
        const val SPAN_COUNT = 2
        const val LOAD_MORE_THRESHOLD = 4
    }
}
