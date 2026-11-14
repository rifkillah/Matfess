package com.matfess.app.ui.tasks

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matfess.app.R
import com.matfess.app.data.Task
import com.matfess.app.databinding.ItemTaskBinding
import com.matfess.app.util.DateUtils
import com.matfess.app.util.Palette

class TaskAdapter(
    private val onClick: (Task) -> Unit,
    private val onToggle: (Task, Boolean) -> Unit,
    private val onLongClick: (Task) -> Unit
) : ListAdapter<Task, TaskAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemTaskBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(private val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(task: Task) {
            val context = binding.root.context
            val overdue = !task.done && task.deadline < System.currentTimeMillis()
            binding.tvTitle.text = task.title
            binding.tvTitle.paintFlags = if (task.done) {
                binding.tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            } else {
                binding.tvTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            }
            binding.tvTitle.alpha = if (task.done) 0.55f else 1f
            binding.tvCourse.text = task.course.ifBlank { "Umum" }
            binding.tvCourse.isVisible = task.course.isNotBlank()
            binding.tvDeadline.text = when {
                task.done -> "Selesai"
                overdue -> "Terlambat · ${DateUtils.formatShort(task.deadline)}"
                else -> DateUtils.relativeDeadline(task.deadline)
            }
            binding.tvDeadline.setTextColor(
                ContextCompat.getColor(
                    context,
                    if (overdue) R.color.danger else R.color.text_tertiary
                )
            )
            binding.priorityDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Palette.priorityColor(context, task.priority)
            )
            binding.checkWrap.background = ContextCompat.getDrawable(
                context,
                if (task.done) R.drawable.bg_selected_check else R.drawable.bg_circle_soft
            )
            binding.iconCheck.isVisible = task.done
            binding.root.alpha = if (task.done) 0.8f else 1f

            binding.checkWrap.setOnClickListener {
                onToggle(task, !task.done)
            }
            binding.root.setOnClickListener { onClick(task) }
            binding.root.setOnLongClickListener {
                onLongClick(task)
                true
            }
            binding.root.animation = null
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Task>() {
            override fun areItemsTheSame(oldItem: Task, newItem: Task) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Task, newItem: Task) = oldItem == newItem
        }
    }
}
