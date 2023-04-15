package com.mvvmmovie.view

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.mvvmmovie.adapter.MovieAdapter
import com.mvvmmovie.databinding.ActivityMainBinding
import com.mvvmmovie.viewmodel.MovieViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    private lateinit var viewModel: MovieViewModel
    private lateinit var movieAdapter : MovieAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //Prepare Adapter/Recycler View UI
        prepareRecyclerView(this@MainActivity)
        // Setup View Model
        viewModel = ViewModelProvider(this@MainActivity)[MovieViewModel::class.java]
        // Call API
        viewModel.getPopularMovies(this@MainActivity)
        //Observe data got result set into adapter
        viewModel.observeMovieLiveData().observe(this@MainActivity) { movieList ->
            movieAdapter.setMovieList(movieList)
        }
    }

    private fun prepareRecyclerView(context: Context) {
        movieAdapter = MovieAdapter(context, MovieAdapter.OnClickListener { movie ->
            // viewModel.toastMessage(this@MainActivity, movie)
            viewModel.intentClass(this@MainActivity, movie)
        })
        binding.rvMovies.apply {
            layoutManager = GridLayoutManager(applicationContext,2)
            adapter = movieAdapter
        }
    }
}