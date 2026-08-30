package com.mvvmmovie.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mvvmmovie.R
import com.mvvmmovie.Utils
import com.mvvmmovie.databinding.ItemMovieBinding
import com.mvvmmovie.model.Movie

class MovieAdapter(
    private val onMovieClick: (Movie) -> Unit
) : ListAdapter<Movie, MovieAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemMovieBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // Passing parent keeps the item's own layout params instead of dropping them.
        return ViewHolder(ItemMovieBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movie = getItem(position)
        Glide.with(holder.itemView)
            .load(Utils.IMAGE_URL + movie.poster_path)
            .placeholder(R.drawable.ic_no_exist)
            .error(R.drawable.ic_no_exist)
            .into(holder.binding.ivMovieImage)
        holder.binding.tvMovieName.text = movie.title
        holder.binding.ivMovieImage.contentDescription = movie.title
        holder.itemView.setOnClickListener { onMovieClick(movie) }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Movie>() {
            override fun areItemsTheSame(oldItem: Movie, newItem: Movie) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Movie, newItem: Movie) =
                oldItem.title == newItem.title && oldItem.poster_path == newItem.poster_path
        }
    }
}
