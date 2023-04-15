package com.mvvmmovie.view

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.mvvmmovie.R
import com.mvvmmovie.Utils
import com.mvvmmovie.databinding.ActivityViewMovieBinding
import com.mvvmmovie.model.Movie
import com.mvvmmovie.viewmodel.ViewMovieViewModel

class ViewMovieActivity : AppCompatActivity() {

    private lateinit var binding : ActivityViewMovieBinding
    private lateinit var viewModel: ViewMovieViewModel

    private lateinit var movieItem: Movie

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this@ViewMovieActivity)[ViewMovieViewModel::class.java]
        movieItem = viewModel.getMovie(intent)
        setupUI(movieItem)
    }

    @SuppressLint("CheckResult")
    private fun setupUI(movie: Movie){
        binding.tvTitle.text = movie.title
        binding.tvOverview.text = movie.overview
        Glide.with(this@ViewMovieActivity).apply { RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true) }
            .load(Utils.imageURL + movie.poster_path)
            .placeholder(R.drawable.ic_no_exist).dontAnimate().into(binding.ivImage)
    }
}