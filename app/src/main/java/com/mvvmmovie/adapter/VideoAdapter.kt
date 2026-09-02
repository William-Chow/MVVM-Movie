package com.mvvmmovie.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mvvmmovie.databinding.ItemVideoBinding
import com.mvvmmovie.model.Video

class VideoAdapter(
    private val onVideoClick: (Video) -> Unit
) : ListAdapter<Video, VideoAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemVideoBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemVideoBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val video = getItem(position)
        holder.binding.tvVideoName.text = video.name
        holder.binding.tvVideoType.text = video.type
        holder.itemView.setOnClickListener { onVideoClick(video) }
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Video>() {
            override fun areItemsTheSame(oldItem: Video, newItem: Video) = oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Video, newItem: Video) =
                oldItem.name == newItem.name && oldItem.key == newItem.key
        }
    }
}
