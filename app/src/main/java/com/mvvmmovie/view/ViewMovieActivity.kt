package com.mvvmmovie.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
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

    private lateinit var binding: ActivityViewMovieBinding
    private lateinit var viewModel: ViewMovieViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        viewModel = ViewModelProvider(this@ViewMovieActivity)[ViewMovieViewModel::class.java]
        val movie = viewModel.movie
        if (movie == null) {
            // No movie in the extras, so there is nothing to render.
            finish()
            return
        }
        setupUI(movie)
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupUI(movie: Movie) {
        title = movie.title
        binding.tvTitle.text = movie.title
        binding.tvMeta.text = buildMeta(movie)
        binding.tvOverview.text = movie.overview?.takeIf { it.isNotBlank() }
            ?: getString(R.string.no_overview)

        // .apply(RequestOptions) is the Glide call; the Kotlin scope function that used to
        // be here built the options and threw them away.
        Glide.with(this@ViewMovieActivity)
            .load(Utils.IMAGE_URL + movie.poster_path)
            .apply(RequestOptions().diskCacheStrategy(DiskCacheStrategy.ALL))
            .placeholder(R.drawable.ic_no_exist)
            .error(R.drawable.ic_no_exist)
            .dontAnimate()
            .into(binding.ivImage)
        binding.ivImage.contentDescription = movie.title
    }

    /** Rating and release date, skipping whichever the API left out. */
    private fun buildMeta(movie: Movie): String = listOfNotNull(
        movie.vote_average?.takeIf { it > 0 }?.let { getString(R.string.rating_format, it) },
        movie.release_date?.takeIf { it.isNotBlank() }
    ).joinToString(SEPARATOR)

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            windowInsets
        }
    }

    private companion object {
        const val SEPARATOR = "  ·  "
    }
}
