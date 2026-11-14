package com.matfess.app.ui.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.LinearLayoutManager
import com.matfess.app.R
import com.matfess.app.databinding.FragmentNotesBinding
import com.matfess.app.ui.common.BaseFragment

class NotesFragment : BaseFragment() {

    private var _binding: FragmentNotesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: NoteAdapter
    private var allNotes: List<com.matfess.app.data.Note> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = NoteAdapter(
            onClick = { note -> showNoteEditor(note) { reload() } },
            onLongClick = { note ->
                repo.db.toggleNotePin(note.id, !note.pinned)
                reload()
                toast(if (note.pinned) "Pin dilepas." else "Catatan disematkan.")
            }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.btnAdd.setOnClickListener {
            showNoteEditor(null) { reload() }
        }
        binding.etSearch.doAfterTextChanged { reload() }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) reload()
    }

    private fun reload() {
        val b = _binding ?: return
        allNotes = repo.db.getNotes()
        val query = b.etSearch.text?.toString()?.trim().orEmpty()
        val filtered = if (query.isBlank()) {
            allNotes
        } else {
            allNotes.filter {
                it.title.contains(query, true) ||
                    it.content.contains(query, true) ||
                    it.course.contains(query, true)
            }
        }
        adapter.submitList(filtered)
        b.recycler.isVisible = filtered.isNotEmpty()
        b.tvEmpty.isVisible = filtered.isEmpty()
        b.tvEmpty.text = if (allNotes.isEmpty()) {
            "Belum ada catatan. Tekan tombol + untuk menulis catatan pertamamu."
        } else {
            "Tidak ada catatan yang cocok dengan “$query”."
        }
        b.tvCount.text = if (allNotes.isEmpty()) {
            "Belum ada catatan tersimpan"
        } else {
            "${filtered.size} dari ${allNotes.size} catatan · ${allNotes.count { it.pinned }} disematkan"
        }
    }

    override fun onDestroyView() {
        _binding?.recycler?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
