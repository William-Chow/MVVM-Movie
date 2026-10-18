package com.mvvmmovie

import android.widget.ImageView
import com.bumptech.glide.Glide

class Utils {

    companion object {
        const val IMAGE_URL = BuildConfig.TMDB_IMAGE_URL

        /** Watch link for a YouTube video key returned by TMDB's videos endpoint. */
        fun youTubeUrl(key: String): String = "https://www.youtube.com/watch?v=$key"
    }
}

/**
 * Loads a TMDB poster or profile path into this view. TMDB leaves the path out for many movies
 * and most cast members; Glide treats a null model as a failed load, so the placeholder shows
 * without a request for ".../null".
 */
fun ImageView.loadTmdbImage(path: String?) {
    Glide.with(this)
        .load(path?.let { Utils.IMAGE_URL + it })
        .placeholder(R.drawable.ic_no_exist)
        .error(R.drawable.ic_no_exist)
        .into(this)
}
