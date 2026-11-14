package com.matfess.app.ui.tools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matfess.app.data.CampusEvent
import com.matfess.app.databinding.ItemEventBinding
import com.matfess.app.util.DateUtils
import com.matfess.app.util.Palette

class EventAdapter(
    private val onClick: (CampusEvent) -> Unit,
    private val onLongClick: (CampusEvent) -> Unit
) : ListAdapter<CampusEvent, EventAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemEventBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemEventBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(event: CampusEvent) {
            val context = binding.root.context
            val accentIndex = when (event.type) {
                "ukt" -> 3
                "krs" -> 4
                "lainnya" -> 5
                else -> 1
            }
            val accent = Palette.accent(context, accentIndex)
            binding.tvDayNumber.text = DateUtils.dayOfMonth(event.date).toString()
            binding.tvMonth.text = DateUtils.formatMonthShort(event.date)
            binding.tvMonth.setTextColor(accent)
            binding.dateWrap.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 34))
            binding.tvTitle.text = event.title
            binding.tvMeta.text = buildString {
                append(DateUtils.formatTime(event.date))
                if (event.location.isNotBlank()) append(" · ${event.location}")
            }
            binding.tvType.text = when (event.type) {
                "ukt" -> "PEMBAYARAN UKT"
                "krs" -> "KRS"
                "lainnya" -> "LAINNYA"
                else -> "KAMPUS"
            }
            binding.tvType.setTextColor(accent)
            binding.tvType.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 34))
            binding.root.setGlassTint(Palette.withAlpha(accent, 18))
            binding.root.setOnClickListener { onClick(event) }
            binding.root.setOnLongClickListener { onLongClick(event); true }
            binding.root.animation = null
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<CampusEvent>() {
            override fun areItemsTheSame(oldItem: CampusEvent, newItem: CampusEvent) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: CampusEvent, newItem: CampusEvent) =
                oldItem == newItem
        }
    }
}
