package com.mvvmmovie.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mvvmmovie.R
import com.mvvmmovie.Utils
import com.mvvmmovie.databinding.ItemCastBinding
import com.mvvmmovie.model.Cast

class CastAdapter : ListAdapter<Cast, CastAdapter.ViewHolder>(DIFF_CALLBACK) {

    class ViewHolder(val binding: ItemCastBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(ItemCastBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = getItem(position)
        Glide.with(holder.itemView)
            .load(Utils.IMAGE_URL + member.profile_path)
            .placeholder(R.drawable.ic_no_exist)
            .error(R.drawable.ic_no_exist)
            .into(holder.binding.ivProfile)
        holder.binding.tvName.text = member.name
        holder.binding.tvCharacter.text = member.character
        holder.binding.ivProfile.contentDescription = member.name
    }

    private companion object {
        val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Cast>() {
            override fun areItemsTheSame(oldItem: Cast, newItem: Cast) =
                oldItem.id == newItem.id && oldItem.character == newItem.character

            override fun areContentsTheSame(oldItem: Cast, newItem: Cast) =
                oldItem.name == newItem.name && oldItem.profile_path == newItem.profile_path
        }
    }
}
