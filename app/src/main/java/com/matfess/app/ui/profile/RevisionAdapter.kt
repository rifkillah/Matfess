package com.matfess.app.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matfess.app.R
import com.matfess.app.data.Revision
import com.matfess.app.databinding.ItemRevisionBinding
import com.matfess.app.util.Palette

class RevisionAdapter(
    private val onClick: (Revision) -> Unit,
    private val onLongClick: (Revision) -> Unit
) : ListAdapter<Revision, RevisionAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemRevisionBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemRevisionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(revision: Revision) {
            val context = binding.root.context
            val accent = when (revision.grade) {
                "D", "E" -> Palette.accent(context, 3)
                "C", "C+" -> Palette.accent(context, 4)
                else -> Palette.accent(context, 5)
            }
            binding.tvGrade.text = revision.grade
            binding.tvGrade.setTextColor(accent)
            binding.gradeWrap.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 38))
            binding.tvCourse.text = revision.course
            binding.tvMeta.text = revision.targetSemester.ifBlank { "Belum dijadwalkan" }
            binding.tvNote.text = revision.note
            binding.tvNote.isVisible = revision.note.isNotBlank()
            binding.root.setGlassTint(Palette.withAlpha(accent, 20))
            binding.root.setOnClickListener { onClick(revision) }
            binding.root.setOnLongClickListener { onLongClick(revision); true }
            binding.root.animation = null
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Revision>() {
            override fun areItemsTheSame(oldItem: Revision, newItem: Revision) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Revision, newItem: Revision) =
                oldItem == newItem
        }
    }
}
