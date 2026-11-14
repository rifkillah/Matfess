package com.matfess.app.ui.tools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matfess.app.databinding.ItemGroupBinding
import com.matfess.app.util.Palette

data class GroupResult(val index: Int, val members: List<String>)

class GroupAdapter : ListAdapter<GroupResult, GroupAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemGroupBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(group: GroupResult) {
            val context = binding.root.context
            val accent = Palette.accent(context, group.index)
            binding.tvNumber.text = (group.index + 1).toString()
            binding.badgeWrap.backgroundTintList =
                android.content.res.ColorStateList.valueOf(accent)
            binding.tvGroupTitle.text = "Kelompok ${group.index + 1}"
            binding.tvMembers.text = group.members
                .mapIndexed { index, name -> "${index + 1}. $name" }
                .joinToString("\n")
            binding.root.setGlassTint(Palette.withAlpha(accent, 22))
            binding.root.animation = null
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GroupResult>() {
            override fun areItemsTheSame(oldItem: GroupResult, newItem: GroupResult) =
                oldItem.index == newItem.index

            override fun areContentsTheSame(oldItem: GroupResult, newItem: GroupResult) =
                oldItem == newItem
        }
    }
}
