package com.mvvmmovie.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mvvmmovie.databinding.ItemMovieBinding
import com.mvvmmovie.model.Movie

class MovieAdapter(context: Context) : RecyclerView.Adapter<MovieAdapter.ViewHolder>() {
    private var movieList = ArrayList<Movie>()
    @SuppressLint("NotifyDataSetChanged")
    fun setMovieList(movieList : List<Movie>){
        this.movieList = movieList as ArrayList<Movie>
        notifyDataSetChanged()
    }
    class ViewHolder(val binding : ItemMovieBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemMovieBinding.inflate(LayoutInflater.from(parent.context)))
    }
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(holder.itemView).load("https://image.tmdb.org/t/p/w500"+movieList[position].poster_path).into(holder.binding.ivMovieImage)
        holder.binding.tvMovieName.text = movieList[position].title
    }
    override fun getItemCount(): Int {
        return movieList.size
    }
}