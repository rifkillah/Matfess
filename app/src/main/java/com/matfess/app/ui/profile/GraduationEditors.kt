package com.matfess.app.ui.profile

import android.view.View
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.R
import com.matfess.app.data.GraduationItem
import com.matfess.app.data.MatfessRepository
import com.matfess.app.data.Revision
import com.matfess.app.databinding.SheetGraduationEditorBinding
import com.matfess.app.databinding.SheetRevisionEditorBinding
import com.matfess.app.util.DateUtils
import com.matfess.app.util.expandFully
import com.matfess.app.util.pickDate
import android.widget.Toast

fun Fragment.showGraduationEditor(item: GraduationItem?, onSaved: () -> Unit) {
    val binding = SheetGraduationEditorBinding.inflate(layoutInflater)
    val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
    dialog.setContentView(binding.root)
    dialog.expandFully()

    val repo = MatfessRepository(requireContext())
    val categories = listOf("Skripsi", "Administrasi", "Sertifikasi", "Pengalaman", "Lainnya")
    var categoryIndex = categories.indexOf(item?.category ?: "Skripsi").coerceAtLeast(0)
    var target = item?.targetDate ?: (DateUtils.now() + 30L * DateUtils.DAY_MILLIS)

    binding.tvHeader.text = if (item == null) "Syarat kelulusan baru" else "Ubah syarat kelulusan"
    binding.etTitle.setText(item?.title.orEmpty())
    binding.etNote.setText(item?.note.orEmpty())

    fun render() {
        binding.tvCategory.text = categories[categoryIndex]
        binding.tvTarget.text = DateUtils.formatFull(target)
    }
    render()

    binding.rowCategory.setOnClickListener {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Kategori")
            .setSingleChoiceItems(categories.toTypedArray(), categoryIndex) { picker, which ->
                categoryIndex = which
                render()
                picker.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    binding.rowTarget.setOnClickListener {
        pickDate(target, "Target tanggal") {
            target = it
            render()
        }
    }
    binding.btnDelete.visibility = if (item == null) View.GONE else View.VISIBLE
    binding.btnDelete.setOnClickListener {
        val targetItem = item ?: return@setOnClickListener
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus syarat?")
            .setMessage("\"${targetItem.title}\" akan dihapus dari daftar kelulusan.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repo.db.deleteGraduation(targetItem.id)
                dialog.dismiss()
                onSaved()
                Toast.makeText(requireContext(), "Syarat dihapus.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    binding.btnSave.setOnClickListener {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            binding.etTitle.error = "Nama syarat wajib diisi"
            binding.etTitle.requestFocus()
            return@setOnClickListener
        }
        val data = GraduationItem(
            id = item?.id ?: 0,
            title = title,
            category = categories[categoryIndex],
            targetDate = DateUtils.startOfDay(target),
            done = item?.done ?: false,
            note = binding.etNote.text?.toString()?.trim().orEmpty()
        )
        if (item == null) repo.db.insertGraduation(data) else repo.db.updateGraduation(data)
        dialog.dismiss()
        onSaved()
        Toast.makeText(
            requireContext(),
            if (item == null) "Syarat kelulusan ditambahkan." else "Syarat diperbarui.",
            Toast.LENGTH_SHORT
        ).show()
    }
    dialog.show()
}

fun Fragment.showRevisionEditor(revision: Revision?, onSaved: () -> Unit) {
    val binding = SheetRevisionEditorBinding.inflate(layoutInflater)
    val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
    dialog.setContentView(binding.root)
    dialog.expandFully()

    val repo = MatfessRepository(requireContext())
    val grades = listOf("A", "A-", "B+", "B", "B-", "C+", "C", "D", "E")
    var grade = revision?.grade ?: "C"

    binding.tvHeader.text = if (revision == null) "Mata kuliah ulang" else "Ubah mata kuliah ulang"
    binding.etCourse.setText(revision?.course.orEmpty())
    binding.etTargetSemester.setText(
        revision?.targetSemester ?: "Semester ${repo.prefs.userSemester}"
    )
    binding.etNote.setText(revision?.note.orEmpty())
    binding.tvGrade.text = grade

    binding.rowGrade.setOnClickListener {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Nilai sebelumnya")
            .setSingleChoiceItems(grades.toTypedArray(), grades.indexOf(grade).coerceAtLeast(0)) { picker, which ->
                grade = grades[which]
                binding.tvGrade.text = grade
                picker.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    binding.btnDelete.visibility = if (revision == null) View.GONE else View.VISIBLE
    binding.btnDelete.setOnClickListener {
        val target = revision ?: return@setOnClickListener
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus mata kuliah ulang?")
            .setMessage("\"${target.course}\" akan dihapus dari daftar.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repo.db.deleteRevision(target.id)
                dialog.dismiss()
                onSaved()
                Toast.makeText(requireContext(), "Daftar ulang diperbarui.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    binding.btnSave.setOnClickListener {
        val course = binding.etCourse.text?.toString()?.trim().orEmpty()
        if (course.isEmpty()) {
            binding.etCourse.error = "Nama mata kuliah wajib diisi"
            binding.etCourse.requestFocus()
            return@setOnClickListener
        }
        val data = Revision(
            id = revision?.id ?: 0,
            course = course,
            grade = grade,
            targetSemester = binding.etTargetSemester.text?.toString()?.trim().orEmpty(),
            note = binding.etNote.text?.toString()?.trim().orEmpty()
        )
        if (revision == null) repo.db.insertRevision(data) else repo.db.updateRevision(data)
        dialog.dismiss()
        onSaved()
        Toast.makeText(
            requireContext(),
            if (revision == null) "Mata kuliah ulang dicatat." else "Catatan diperbarui.",
            Toast.LENGTH_SHORT
        ).show()
    }
    dialog.show()
}
