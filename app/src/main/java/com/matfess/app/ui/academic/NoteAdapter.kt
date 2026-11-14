package com.matfess.app.ui.academic

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matfess.app.R
import com.matfess.app.data.Note
import com.matfess.app.databinding.ItemNoteBinding
import com.matfess.app.util.DateUtils
import com.matfess.app.util.Palette

class NoteAdapter(
    private val onClick: (Note) -> Unit,
    private val onLongClick: (Note) -> Unit
) : ListAdapter<Note, NoteAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemNoteBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemNoteBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(note: Note) {
            val context = binding.root.context
            binding.tvTitle.text = note.title
            binding.tvPreview.text = note.content.ifBlank { "Tidak ada isi catatan." }
            binding.tvPreview.isVisible = true
            binding.tvCourse.text = note.course.ifBlank { "Umum" }
            binding.tvCourse.setTextColor(Palette.accent(context, note.color))
            binding.tvDate.text = DateUtils.formatShort(note.date)
            binding.iconPin.isVisible = note.pinned
            binding.accentDot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Palette.accent(context, note.color)
            )
            binding.root.setGlassTint(Palette.soft(context, note.color, 26))
            binding.tvCourse.backgroundTintList = android.content.res.ColorStateList.valueOf(
                Palette.soft(context, note.color, 36)
            )
            binding.root.setOnClickListener { onClick(note) }
            binding.root.setOnLongClickListener { onLongClick(note); true }
            binding.root.animation = null
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Note, newItem: Note) = oldItem == newItem
        }
    }
}
