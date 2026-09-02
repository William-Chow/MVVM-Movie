package com.mvvmmovie

class Utils {

    companion object {
        const val IMAGE_URL = BuildConfig.TMDB_IMAGE_URL

        /** Watch link for a YouTube video key returned by TMDB's videos endpoint. */
        fun youTubeUrl(key: String): String = "https://www.youtube.com/watch?v=$key"
    }
}
