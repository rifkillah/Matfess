package com.matfess.app.ui.profile

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matfess.app.R
import com.matfess.app.data.GraduationItem
import com.matfess.app.databinding.ItemGraduationBinding
import com.matfess.app.util.DateUtils
import com.matfess.app.util.Palette

class GraduationAdapter(
    private val onToggle: (GraduationItem, Boolean) -> Unit,
    private val onClick: (GraduationItem) -> Unit,
    private val onLongClick: (GraduationItem) -> Unit
) : ListAdapter<GraduationItem, GraduationAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemGraduationBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemGraduationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: GraduationItem) {
            val context = binding.root.context
            val accent = when (item.category) {
                "Skripsi" -> Palette.accent(context, 0)
                "Administrasi" -> Palette.accent(context, 1)
                "Sertifikasi" -> Palette.accent(context, 4)
                "Pengalaman" -> Palette.accent(context, 2)
                else -> Palette.accent(context, 5)
            }
            binding.tvTitle.text = item.title
            binding.tvTitle.paintFlags = if (item.done) {
                binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.tvTitle.alpha = if (item.done) 0.6f else 1f
            binding.tvMeta.text = "${DateUtils.dayNameOf(item.targetDate).replaceFirstChar { it.uppercase() }}, " +
                DateUtils.formatShort(item.targetDate)
            binding.tvNote.text = item.note
            binding.tvNote.isVisible = item.note.isNotBlank()
            binding.tvCategory.text = item.category.uppercase()
            binding.tvCategory.setTextColor(accent)
            binding.tvCategory.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 34))
            binding.checkWrap.background = ContextCompat.getDrawable(
                context,
                if (item.done) R.drawable.bg_selected_check else R.drawable.bg_circle_soft
            )
            binding.checkWrap.backgroundTintList = if (item.done) {
                android.content.res.ColorStateList.valueOf(Palette.accent(context, 2))
            } else {
                null
            }
            binding.iconCheck.isVisible = item.done
            binding.root.setGlassTint(Palette.withAlpha(accent, if (item.done) 12 else 22))
            binding.checkWrap.setOnClickListener { onToggle(item, !item.done) }
            binding.root.setOnClickListener { onClick(item) }
            binding.root.setOnLongClickListener { onLongClick(item); true }
            binding.root.animation = null
            binding.root.alpha = if (item.done) 0.82f else 1f
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<GraduationItem>() {
            override fun areItemsTheSame(oldItem: GraduationItem, newItem: GraduationItem) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: GraduationItem, newItem: GraduationItem) =
                oldItem == newItem
        }
    }
}
