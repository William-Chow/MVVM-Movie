package com.mvvmmovie.view

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.MenuProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.mvvmmovie.R
import com.mvvmmovie.Utils
import com.mvvmmovie.adapter.CastAdapter
import com.mvvmmovie.adapter.SimilarMovieAdapter
import com.mvvmmovie.adapter.VideoAdapter
import com.mvvmmovie.databinding.ActivityViewMovieBinding
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.Video
import com.mvvmmovie.viewmodel.MovieDetailUiState
import com.mvvmmovie.viewmodel.ViewMovieViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.util.Locale

@AndroidEntryPoint
class ViewMovieActivity : AppCompatActivity() {

    private lateinit var binding: ActivityViewMovieBinding

    private val viewModel: ViewMovieViewModel by viewModels()

    private val castAdapter = CastAdapter()
    private val similarAdapter = SimilarMovieAdapter { movie -> openMovie(movie) }
    private val videoAdapter = VideoAdapter { video -> playVideo(video) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityViewMovieBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applyWindowInsets()
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.rvCast.adapter = castAdapter
        binding.rvSimilar.adapter = similarAdapter
        binding.rvTrailers.adapter = videoAdapter
        binding.btnRetry.setOnClickListener { viewModel.load() }
        addMenuProvider(favoriteMenuProvider, this@ViewMovieActivity)

        viewModel.observeUiState().observe(this@ViewMovieActivity) { state ->
            binding.progressBar.isVisible = state is MovieDetailUiState.Loading
            binding.errorContainer.isVisible = state is MovieDetailUiState.Error
            binding.content.isVisible = state is MovieDetailUiState.Success

            when (state) {
                is MovieDetailUiState.Success -> setupUI(state.movie)
                is MovieDetailUiState.Error -> binding.tvError.text = when {
                    state.offline -> getString(R.string.error_no_network)
                    else -> state.message ?: getString(R.string.error_unknown)
                }
                MovieDetailUiState.Loading -> Unit
            }
            invalidateOptionsMenu()
        }

        viewModel.isFavorite.observe(this@ViewMovieActivity) { invalidateOptionsMenu() }
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

        binding.tvTagline.setTextOrHide(movie.tagline)
        binding.tvGenres.setTextOrHide(movie.genres?.mapNotNull { it.name }?.joinToString(SEPARATOR))
        binding.tvFinancials.setTextOrHide(buildFinancials(movie))
        binding.tvStudios.setTextOrHide(
            movie.production_companies?.mapNotNull { it.name }?.joinToString(SEPARATOR)
        )

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

        val cast = movie.credits?.cast.orEmpty().take(MAX_CAST)
        castAdapter.submitList(cast)
        binding.tvCastLabel.isVisible = cast.isNotEmpty()
        binding.rvCast.isVisible = cast.isNotEmpty()

        val trailers = movie.videos?.results.orEmpty().filter { it.isYouTube }
        videoAdapter.submitList(trailers)
        binding.tvTrailersLabel.isVisible = trailers.isNotEmpty()
        binding.rvTrailers.isVisible = trailers.isNotEmpty()

        val similar = movie.similar?.results.orEmpty()
        similarAdapter.submitList(similar)
        binding.tvSimilarLabel.isVisible = similar.isNotEmpty()
        binding.rvSimilar.isVisible = similar.isNotEmpty()
    }

    /** Rating, release date and runtime, skipping whichever the API left out. */
    private fun buildMeta(movie: Movie): String = listOfNotNull(
        movie.vote_average?.takeIf { it > 0 }?.let { getString(R.string.rating_format, it) },
        movie.release_date?.takeIf { it.isNotBlank() },
        movie.runtime?.takeIf { it > 0 }?.let(::formatRuntime)
    ).joinToString(SEPARATOR)

    private fun buildFinancials(movie: Movie): String? = listOfNotNull(
        movie.budget?.takeIf { it > 0 }?.let { getString(R.string.budget_format, formatMoney(it)) },
        movie.revenue?.takeIf { it > 0 }?.let { getString(R.string.revenue_format, formatMoney(it)) }
    ).joinToString(SEPARATOR).takeIf { it.isNotEmpty() }

    private fun formatRuntime(minutes: Int): String = if (minutes >= MINUTES_PER_HOUR) {
        getString(R.string.runtime_format, minutes / MINUTES_PER_HOUR, minutes % MINUTES_PER_HOUR)
    } else {
        getString(R.string.runtime_minutes_format, minutes)
    }

    private fun formatMoney(amount: Long): String =
        NumberFormat.getCurrencyInstance(Locale.US).apply { maximumFractionDigits = 0 }.format(amount)

    private fun openMovie(movie: Movie) {
        val id = movie.id ?: return
        val intent = Intent(this@ViewMovieActivity, ViewMovieActivity::class.java)
        intent.putExtra(ViewMovieViewModel.EXTRA_MOVIE_ID, id)
        startActivity(intent)
    }

    private fun playVideo(video: Video) {
        val key = video.key ?: return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Utils.youTubeUrl(key).toUri()))
        } catch (notFound: ActivityNotFoundException) {
            Toast.makeText(this@ViewMovieActivity, R.string.no_video_app, Toast.LENGTH_SHORT).show()
        }
    }

    private val favoriteMenuProvider = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.menu_detail, menu)
        }

        override fun onPrepareMenu(menu: Menu) {
            val item = menu.findItem(R.id.action_favorite) ?: return
            // Only offer the star once there is a movie to attach it to.
            item.isVisible = viewModel.observeUiState().value is MovieDetailUiState.Success
            val favorite = viewModel.isFavorite.value == true
            item.setIcon(
                if (favorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off
            )
            item.setTitle(if (favorite) R.string.action_unfavorite else R.string.action_favorite)
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            if (menuItem.itemId != R.id.action_favorite) return false
            viewModel.toggleFavorite()
            return true
        }
    }

    private fun android.widget.TextView.setTextOrHide(value: String?) {
        text = value
        isVisible = !value.isNullOrBlank()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(left = bars.left, right = bars.right, bottom = bars.bottom)
            windowInsets
        }
    }

    private companion object {
        const val SEPARATOR = "  ·  "
        const val MAX_CAST = 20
        const val MINUTES_PER_HOUR = 60
    }
}
