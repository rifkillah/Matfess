package com.matfess.app.ui.tools

import android.content.Context
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.R
import com.matfess.app.data.CampusEvent
import com.matfess.app.data.MatfessRepository
import com.matfess.app.databinding.SheetEventEditorBinding
import com.matfess.app.util.DateUtils
import com.matfess.app.util.expandFully
import com.matfess.app.util.pickDate
import com.matfess.app.util.pickTime
import android.widget.Toast

fun Fragment.showEventEditor(event: CampusEvent?, onSaved: () -> Unit) {
    val binding = SheetEventEditorBinding.inflate(layoutInflater)
    val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
    dialog.setContentView(binding.root)
    dialog.expandFully()

    val repo = MatfessRepository(requireContext())
    val types = listOf("kampus", "ukt", "krs", "lainnya")
    var typeIndex = types.indexOf(event?.type ?: "kampus").coerceAtLeast(0)
    var date = event?.date ?: DateUtils.combine(
        DateUtils.now() + DateUtils.DAY_MILLIS, 9, 0
    )

    binding.tvHeader.text = if (event == null) "Agenda baru" else "Ubah agenda"
    binding.etTitle.setText(event?.title.orEmpty())
    binding.etLocation.setText(event?.location.orEmpty())
    binding.etNote.setText(event?.note.orEmpty())
    binding.segType.setItems(listOf("Kampus", "UKT", "KRS", "Lainnya"), typeIndex)
    binding.segType.setOnSegmentChanged { typeIndex = it }

    fun renderDate() {
        binding.tvDate.text = "${DateUtils.formatFull(date)} · ${DateUtils.formatTime(date)}"
    }
    renderDate()

    binding.rowDate.setOnClickListener {
        pickDate(date, "Tanggal agenda") { picked ->
            val time = DateUtils.toLocal(date)
            pickTime(time.hour, time.minute, "Waktu agenda") { hour, minute ->
                date = DateUtils.combine(picked, hour, minute)
                renderDate()
            }
        }
    }
    binding.btnDelete.visibility = if (event == null) View.GONE else View.VISIBLE
    binding.btnDelete.setOnClickListener {
        val target = event ?: return@setOnClickListener
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus agenda?")
            .setMessage("\"${target.title}\" akan dihapus dari kalender kampus.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repo.db.deleteEvent(target.id)
                dialog.dismiss()
                onSaved()
                Toast.makeText(requireContext(), "Agenda dihapus.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    binding.btnSave.setOnClickListener {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            binding.etTitle.error = "Nama agenda wajib diisi"
            binding.etTitle.requestFocus()
            return@setOnClickListener
        }
        val item = CampusEvent(
            id = event?.id ?: 0,
            title = title,
            date = date,
            location = binding.etLocation.text?.toString()?.trim().orEmpty(),
            note = binding.etNote.text?.toString()?.trim().orEmpty(),
            type = types[typeIndex]
        )
        if (event == null) repo.db.insertEvent(item) else repo.db.updateEvent(item)
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
        dialog.dismiss()
        onSaved()
        Toast.makeText(
            requireContext(),
            if (event == null) "Agenda ditambahkan." else "Agenda diperbarui.",
            Toast.LENGTH_SHORT
        ).show()
    }
    dialog.show()
}
