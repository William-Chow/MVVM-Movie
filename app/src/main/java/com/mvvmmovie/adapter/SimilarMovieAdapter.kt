package com.mvvmmovie.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mvvmmovie.R
import com.mvvmmovie.Utils
import com.mvvmmovie.databinding.ItemMovieCompactBinding
import com.mvvmmovie.model.Movie

/** Compact, fixed-width poster used by the horizontal "similar titles" row. */
class SimilarMovieAdapter(
    private val onMovieClick: (Movie) -> Unit
) : ListAdapter<Movie, SimilarMovieAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemMovieCompactBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemMovieCompactBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val movie = getItem(position)
        Glide.with(holder.itemView)
            .load(Utils.IMAGE_URL + movie.poster_path)
            .placeholder(R.drawable.ic_no_exist)
            .error(R.drawable.ic_no_exist)
            .into(holder.binding.ivPoster)
        holder.binding.tvName.text = movie.title
        holder.binding.ivPoster.contentDescription = movie.title
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
