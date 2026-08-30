package com.mvvmmovie.view

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mvvmmovie.R
import com.mvvmmovie.adapter.MovieAdapter
import com.mvvmmovie.databinding.ActivityMainBinding
import com.mvvmmovie.model.Movie
import com.mvvmmovie.viewmodel.MovieUiState
import com.mvvmmovie.viewmodel.MovieViewModel
import com.mvvmmovie.viewmodel.ViewMovieViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MovieViewModel
    private lateinit var movieAdapter: MovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()

        // Setup View Model. The first request is fired from its init block, so a
        // rotation no longer re-runs it.
        viewModel = ViewModelProvider(this@MainActivity)[MovieViewModel::class.java]

        prepareRecyclerView()
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
        binding.btnRetry.setOnClickListener { viewModel.refresh() }
        addMenuProvider(searchMenuProvider, this@MainActivity)

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

            when (state) {
                is MovieUiState.Success -> movieAdapter.submitList(state.movies)
                is MovieUiState.Empty -> movieAdapter.submitList(emptyList())
                is MovieUiState.Error -> {
                    binding.tvError.text = state.message ?: getString(R.string.error_unknown)
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
    }

    /** Navigation belongs to the view, not to the ViewModel. */
    private fun openMovie(movie: Movie) {
        val intent = Intent(this@MainActivity, ViewMovieActivity::class.java)
        intent.putExtra(ViewMovieViewModel.EXTRA_MOVIE, movie)
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

    private val searchMenuProvider = object : MenuProvider {
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

        override fun onMenuItemSelected(menuItem: MenuItem) = false
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
