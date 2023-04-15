package com.mvvmmovie.viewmodel

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.Movies
import com.mvvmmovie.network.RetrofitClient
import com.mvvmmovie.view.MainActivity
import com.mvvmmovie.view.ViewMovieActivity
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.Serializable

class MovieViewModel : ViewModel() {
    private var movieLiveData = MutableLiveData<List<Movie>>()

    fun getPopularMovies(context: Context) {
        RetrofitClient.api.getPopularMovies(RetrofitClient.API_KEY).enqueue(object :
            Callback<Movies> {
            override fun onResponse(call: Call<Movies>, response: Response<Movies>) {
                if (response.body() != null) {
                    movieLiveData.value = response.body()!!.results
                } else {
                    // No Result
                    return
                }
            }

            override fun onFailure(call: Call<Movies>, t: Throwable) {
                // Log.d("William", t.message.toString())
                Toast.makeText(context, t.message.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun observeMovieLiveData() : LiveData<List<Movie>> {
        return movieLiveData
    }

    fun toastMessage(mainActivity: MainActivity, movie: Movie) {
        Toast.makeText(mainActivity, "${movie.original_title}", Toast.LENGTH_SHORT).show()
    }

    fun intentClass(mainActivity: MainActivity, movie: Movie) {
        val intent = Intent(mainActivity, ViewMovieActivity::class.java)
        intent.putExtra("Movie", movie as Serializable)
        mainActivity.startActivity(intent)
    }
}