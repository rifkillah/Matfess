package com.matfess.app.ui.schedule

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matfess.app.R
import com.matfess.app.data.Course
import com.matfess.app.databinding.ItemScheduleBinding
import com.matfess.app.util.DateUtils
import com.matfess.app.util.Palette

class ScheduleAdapter(
    private val compact: Boolean = false,
    private val onClick: (Course) -> Unit,
    private val onLongClick: (Course) -> Unit
) : ListAdapter<Course, ScheduleAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemScheduleBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemScheduleBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(course: Course) {
            val context = binding.root.context
            val accent = Palette.accent(context, course.color)
            binding.accentBar.backgroundTintList =
                android.content.res.ColorStateList.valueOf(accent)
            binding.tvDay.text = DateUtils.dayShort(course.day).uppercase()
            binding.tvDay.setTextColor(accent)
            binding.tvStart.text = course.startTime
            binding.tvEnd.text = course.endTime
            binding.tvName.text = course.name
            binding.tvRoom.text = buildString {
                if (course.room.isNotBlank()) append(course.room)
                if (course.room.isNotBlank() && course.lecturer.isNotBlank()) append(" · ")
                if (course.lecturer.isNotBlank()) append(course.lecturer)
            }.ifBlank { "Ruang & dosen belum diisi" }
            binding.tvLecturer.text = if (course.phone.isNotBlank()) {
                "Kontak dosen tersedia"
            } else {
                if (course.lecturer.isNotBlank()) "Dosen: ${course.lecturer}" else "Kontak belum diisi"
            }

            val showActions = !compact && course.phone.isNotBlank()
            binding.btnChat.visibility = if (showActions) android.view.View.VISIBLE else android.view.View.GONE
            binding.btnCall.visibility = if (showActions) android.view.View.VISIBLE else android.view.View.GONE

            binding.btnChat.setOnClickListener {
                if (course.phone.isBlank()) return@setOnClickListener
                val number = course.phone.replace(Regex("[^0-9+]"), "")
                val normalized = if (number.startsWith("0")) "62${number.substring(1)}" else number
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$normalized"))
                    )
                }
            }
            binding.btnCall.setOnClickListener {
                if (course.phone.isBlank()) return@setOnClickListener
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${course.phone}")))
                }
            }
            binding.root.setOnClickListener { onClick(course) }
            binding.root.setOnLongClickListener {
                onLongClick(course)
                true
            }
            binding.root.setGlassTint(
                ColorUtils.setAlphaComponent(
                    ContextCompat.getColor(context, R.color.glass_top), 40
                )
            )
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Course>() {
            override fun areItemsTheSame(oldItem: Course, newItem: Course) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Course, newItem: Course) = oldItem == newItem
        }
    }
}
