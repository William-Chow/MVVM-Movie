package com.mvvmmovie.viewmodel

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.mvvmmovie.model.Movie

class ViewMovieViewModel : ViewModel() {

    fun getMovie(intent: Intent) : Movie{
        return intent.getSerializableExtra("Movie") as Movie
    }

}