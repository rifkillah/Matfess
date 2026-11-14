package com.matfess.app.ui.academic

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.matfess.app.R
import com.matfess.app.data.Document
import com.matfess.app.databinding.ItemDocumentBinding
import com.matfess.app.util.DateUtils
import com.matfess.app.util.Palette

class DocumentAdapter(
    private val onOpen: (Document) -> Unit,
    private val onMore: (Document) -> Unit
) : ListAdapter<Document, DocumentAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(ItemDocumentBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(getItem(position))

    inner class VH(private val binding: ItemDocumentBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(document: Document) {
            val context = binding.root.context
            val isCertificate = document.type == "certificate"
            val accent = Palette.accent(context, if (isCertificate) 4 else 0)
            binding.tvTitle.text = document.title
            binding.tvMeta.text = buildString {
                if (document.course.isNotBlank()) append(document.course)
                if (document.course.isNotBlank()) append(" · ")
                append(DateUtils.formatShort(document.addedAt))
            }
            binding.tvType.text = if (isCertificate) "SERTIFIKAT" else "MODUL"
            binding.tvType.setTextColor(accent)
            binding.tvType.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 34))
            binding.iconType.setImageResource(
                if (isCertificate) R.drawable.ic_trophy else R.drawable.ic_book
            )
            binding.iconType.imageTintList = android.content.res.ColorStateList.valueOf(accent)
            binding.iconWrap.backgroundTintList =
                android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 26))
            binding.root.setGlassTint(Palette.soft(context, if (isCertificate) 4 else 0, 22))
            binding.root.setOnClickListener { onOpen(document) }
            binding.btnMore.setOnClickListener { onMore(document) }
            binding.root.animation = null
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Document>() {
            override fun areItemsTheSame(oldItem: Document, newItem: Document) =
                oldItem.id == newItem.id

            override fun areContentsTheSame(oldItem: Document, newItem: Document) =
                oldItem == newItem
        }
    }
}
