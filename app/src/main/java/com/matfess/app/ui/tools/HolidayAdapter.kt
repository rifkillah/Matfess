package com.matfess.app.ui.tools

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matfess.app.databinding.ItemHolidayBinding
import com.matfess.app.network.Holiday
import com.matfess.app.util.DateUtils
import com.matfess.app.util.Palette

class HolidayAdapter : ListAdapter<Holiday, HolidayAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemHolidayBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemHolidayBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(holiday: Holiday) {
            val context = binding.root.context
            val accent = Palette.accent(context, 3)
            binding.tvDayNumber.text = DateUtils.dayOfMonth(holiday.date).toString()
            binding.tvMonth.text = DateUtils.formatMonthShort(holiday.date)
            binding.tvMonth.setTextColor(accent)
            binding.dateWrap.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 34))
            binding.tvTitle.text = holiday.localName
            binding.tvEnglish.text = holiday.englishName
            binding.tvEnglish.visibility =
                if (holiday.englishName.isBlank() || holiday.englishName == holiday.localName) {
                    android.view.View.GONE
                } else {
                    android.view.View.VISIBLE
                }
            binding.tvBadge.setTextColor(accent)
            binding.tvBadge.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 34))
            binding.root.setGlassTint(Palette.withAlpha(accent, 16))
            binding.root.animation = null
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Holiday>() {
            override fun areItemsTheSame(oldItem: Holiday, newItem: Holiday) =
                oldItem.date == newItem.date && oldItem.localName == newItem.localName

            override fun areContentsTheSame(oldItem: Holiday, newItem: Holiday) =
                oldItem == newItem
        }
    }
}
