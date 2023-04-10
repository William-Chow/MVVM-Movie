package com.mvvmmovie.viewmodel

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.mvvmmovie.model.Movie
import com.mvvmmovie.model.Movies
import com.mvvmmovie.network.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

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
                Log.d("William", t.message.toString())
                Toast.makeText(context, t.message.toString(), Toast.LENGTH_SHORT).show()
            }
        })
    }

    fun observeMovieLiveData() : LiveData<List<Movie>> {
        return movieLiveData
    }
}