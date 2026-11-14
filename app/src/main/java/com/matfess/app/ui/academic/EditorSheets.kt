package com.matfess.app.ui.academic

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.provider.OpenableColumns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import androidx.core.graphics.ColorUtils
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.R
import com.matfess.app.data.Course
import com.matfess.app.data.Document
import com.matfess.app.data.MatfessRepository
import com.matfess.app.data.Note
import com.matfess.app.data.Task
import com.matfess.app.databinding.SheetCourseEditorBinding
import com.matfess.app.databinding.SheetDocumentEditorBinding
import com.matfess.app.databinding.SheetNoteEditorBinding
import com.matfess.app.databinding.SheetTaskEditorBinding
import com.matfess.app.service.AlarmScheduler
import com.matfess.app.util.DateUtils
import com.matfess.app.util.expandFully
import com.matfess.app.util.Palette
import com.matfess.app.util.dp
import com.matfess.app.util.pickDate
import com.matfess.app.util.pickTime
import android.widget.Toast

/**
 * All editor sheets for the academic hub live here so add/edit flows behave
 * identically and every change is written to SQLite immediately.
 */
private fun Fragment.repository(): MatfessRepository = MatfessRepository(requireContext())

private fun Fragment.toast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

private fun BottomSheetDialog.prepare() {
    window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
}

private fun Context.displayName(uri: Uri): String {
    return runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) cursor.getString(index) else null
                } else null
            }
    }.getOrNull() ?: uri.lastPathSegment?.substringAfterLast('/') ?: "Berkas"
}

private fun Fragment.confirmDelete(label: String, onConfirm: () -> Unit) {
    MaterialAlertDialogBuilder(requireContext())
        .setTitle("Hapus $label?")
        .setMessage("Tindakan ini tidak bisa dibatalkan.")
        .setNegativeButton("Batal", null)
        .setPositiveButton("Hapus") { _, _ -> onConfirm() }
        .show()
}

// ------------------------------------------------------------------ notes
fun Fragment.showNoteEditor(note: Note?, onSaved: () -> Unit) {
    val binding = SheetNoteEditorBinding.inflate(layoutInflater)
    val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
    dialog.setContentView(binding.root)
    dialog.expandFully()

    val repo = repository()
    var color = note?.color ?: 0

    binding.tvHeader.text = if (note == null) "Catatan baru" else "Ubah catatan"
    binding.etTitle.setText(note?.title.orEmpty())
    binding.etCourse.setText(note?.course.orEmpty())
    binding.etContent.setText(note?.content.orEmpty())
    binding.switchPin.setChecked(note?.pinned ?: false, notify = false, animate = false)

    fun renderColors() {
        binding.colorRow.removeAllViews()
        Palette.accents(requireContext()).forEachIndexed { index, accent ->
            val dot = View(requireContext())
            val size = requireContext().dp(32)
            dot.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                marginEnd = requireContext().dp(10)
            }
            dot.background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(accent)
                if (index == color) {
                    setStroke(requireContext().dp(3), Color.WHITE)
                } else {
                    setStroke(
                        requireContext().dp(1),
                        ColorUtils.setAlphaComponent(Color.WHITE, 80)
                    )
                }
            }
            dot.scaleX = if (index == color) 1.08f else 0.86f
            dot.scaleY = if (index == color) 1.08f else 0.86f
            dot.contentDescription = "Warna ${index + 1}"
            dot.setOnClickListener {
                color = index
                renderColors()
            }
            binding.colorRow.addView(dot)
        }
    }
    renderColors()

    binding.btnDelete.visibility = if (note == null) View.GONE else View.VISIBLE
    binding.btnDelete.setOnClickListener {
        val target = note ?: return@setOnClickListener
        confirmDelete("catatan ini") {
            repo.db.deleteNote(target.id)
            dialog.dismiss()
            onSaved()
            toast("Catatan dihapus.")
        }
    }
    binding.btnSave.setOnClickListener {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val content = binding.etContent.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            binding.etTitle.error = "Judul wajib diisi"
            binding.etTitle.requestFocus()
            return@setOnClickListener
        }
        val item = Note(
            id = note?.id ?: 0,
            title = title,
            content = content,
            course = binding.etCourse.text?.toString()?.trim().orEmpty(),
            date = note?.date ?: DateUtils.now(),
            color = color,
            pinned = binding.switchPin.isChecked()
        )
        if (note == null) repo.db.insertNote(item) else repo.db.updateNote(item)
        hideKeyboard(binding.root)
        dialog.dismiss()
        onSaved()
        toast(if (note == null) "Catatan disimpan." else "Catatan diperbarui.")
    }
    dialog.show()
}

// ------------------------------------------------------------------ tasks
fun Fragment.showTaskEditor(task: Task?, onSaved: () -> Unit) {
    val binding = SheetTaskEditorBinding.inflate(layoutInflater)
    val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
    dialog.setContentView(binding.root)
    dialog.expandFully()

    val repo = repository()
    val context = requireContext()
    val now = DateUtils.now()
    var deadline = task?.deadline ?: run {
        val evening = DateUtils.combine(now, 20, 0)
        if (evening > now) evening else DateUtils.combine(now + DateUtils.DAY_MILLIS, 20, 0)
    }
    var reminder = task?.reminderMinutes ?: 30
    var priority = task?.priority ?: 1

    binding.tvHeader.text = if (task == null) "Tugas baru" else "Ubah tugas"
    binding.etTitle.setText(task?.title.orEmpty())
    binding.etCourse.setText(task?.course.orEmpty())
    binding.etDescription.setText(task?.description.orEmpty())
    binding.segPriority.setItems(listOf("Rendah", "Sedang", "Tinggi"), priority)
    binding.segPriority.setOnSegmentChanged { priority = it }

    fun renderDeadline() {
        binding.tvDeadline.text = "${DateUtils.formatFull(deadline)} · ${DateUtils.formatTime(deadline)}"
    }
    fun renderReminder() {
        binding.tvReminder.text = when (reminder) {
            10 -> "10 menit sebelum deadline"
            30 -> "30 menit sebelum deadline"
            60 -> "1 jam sebelum deadline"
            120 -> "2 jam sebelum deadline"
            1440 -> "1 hari sebelum deadline"
            else -> "$reminder menit sebelum deadline"
        }
    }
    renderDeadline()
    renderReminder()

    binding.rowDeadline.setOnClickListener {
        pickDate(deadline, "Tanggal deadline") { date ->
            val time = DateUtils.toLocal(deadline)
            pickTime(time.hour, time.minute, "Jam deadline") { hour, minute ->
                deadline = DateUtils.combine(date, hour, minute)
                renderDeadline()
            }
        }
    }
    binding.rowReminder.setOnClickListener {
        val labels = arrayOf(
            "10 menit sebelum", "30 menit sebelum", "1 jam sebelum",
            "2 jam sebelum", "1 hari sebelum"
        )
        val values = intArrayOf(10, 30, 60, 120, 1440)
        val checked = values.indexOf(reminder).coerceAtLeast(1)
        MaterialAlertDialogBuilder(context)
            .setTitle("Pengingat deadline")
            .setSingleChoiceItems(labels, checked) { picker, which ->
                reminder = values[which]
                renderReminder()
                picker.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    binding.btnDelete.visibility = if (task == null) View.GONE else View.VISIBLE
    binding.btnDelete.setOnClickListener {
        val target = task ?: return@setOnClickListener
        confirmDelete("tugas ini") {
            repo.db.deleteTask(target.id)
            AlarmScheduler.cancelTask(context, target.id)
            dialog.dismiss()
            onSaved()
            toast("Tugas dihapus.")
        }
    }
    binding.btnSave.setOnClickListener {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        if (title.isEmpty()) {
            binding.etTitle.error = "Nama tugas wajib diisi"
            binding.etTitle.requestFocus()
            return@setOnClickListener
        }
        val item = Task(
            id = task?.id ?: 0,
            title = title,
            course = binding.etCourse.text?.toString()?.trim().orEmpty(),
            description = binding.etDescription.text?.toString()?.trim().orEmpty(),
            deadline = deadline,
            done = task?.done ?: false,
            priority = priority,
            reminderMinutes = reminder
        )
        val id = if (task == null) repo.db.insertTask(item) else {
            repo.db.updateTask(item)
            item.id
        }
        val saved = item.copy(id = id)
        AlarmScheduler.scheduleTask(context, saved)
        hideKeyboard(binding.root)
        dialog.dismiss()
        onSaved()
        toast(
            if (saved.deadline > System.currentTimeMillis()) {
                "Tugas disimpan · alarm disiapkan."
            } else {
                "Tugas disimpan, tetapi deadlinenya sudah lewat."
            }
        )
    }
    dialog.show()
}

// ---------------------------------------------------------------- courses
fun Fragment.showCourseEditor(course: Course?, onSaved: () -> Unit) {
    val binding = SheetCourseEditorBinding.inflate(layoutInflater)
    val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
    dialog.setContentView(binding.root)
    dialog.expandFully()

    val repo = repository()
    val context = requireContext()
    var day = course?.day ?: DateUtils.dayIndex()
    var start = course?.startTime ?: "08:00"
    var end = course?.endTime ?: "09:40"

    binding.tvHeader.text = if (course == null) "Mata kuliah baru" else "Ubah mata kuliah"
    binding.etName.setText(course?.name.orEmpty())
    binding.etRoom.setText(course?.room.orEmpty())
    binding.etLecturer.setText(course?.lecturer.orEmpty())
    binding.etPhone.setText(course?.phone.orEmpty())
    binding.switchReminder.setChecked(course?.reminder ?: true, notify = false, animate = false)

    fun render() {
        binding.tvDay.text = DateUtils.dayName(day)
        binding.tvStart.text = start
        binding.tvEnd.text = end
    }
    render()

    binding.rowDay.setOnClickListener {
        val days = (1..7).map { DateUtils.dayName(it) }.toTypedArray()
        MaterialAlertDialogBuilder(context)
            .setTitle("Hari kuliah")
            .setSingleChoiceItems(days, day - 1) { picker, which ->
                day = which + 1
                render()
                picker.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }
    binding.rowStart.setOnClickListener {
        val (h, m) = DateUtils.parseTime(start)
        pickTime(h, m, "Jam mulai") { hour, minute ->
            start = DateUtils.formatTime(hour, minute)
            render()
        }
    }
    binding.rowEnd.setOnClickListener {
        val (h, m) = DateUtils.parseTime(end)
        pickTime(h, m, "Jam selesai") { hour, minute ->
            end = DateUtils.formatTime(hour, minute)
            render()
        }
    }
    binding.btnDelete.visibility = if (course == null) View.GONE else View.VISIBLE
    binding.btnDelete.setOnClickListener {
        val target = course ?: return@setOnClickListener
        confirmDelete("mata kuliah ini") {
            repo.db.deleteCourse(target.id)
            AlarmScheduler.cancelClassReminder(context, target.id)
            dialog.dismiss()
            onSaved()
            toast("Jadwal dihapus.")
        }
    }
    binding.btnSave.setOnClickListener {
        val name = binding.etName.text?.toString()?.trim().orEmpty()
        if (name.isEmpty()) {
            binding.etName.error = "Nama mata kuliah wajib diisi"
            binding.etName.requestFocus()
            return@setOnClickListener
        }
        if (end <= start) {
            toast("Jam selesai harus lebih lambat dari jam mulai.")
            return@setOnClickListener
        }
        val item = Course(
            id = course?.id ?: 0,
            name = name,
            day = day,
            startTime = start,
            endTime = end,
            room = binding.etRoom.text?.toString()?.trim().orEmpty(),
            lecturer = binding.etLecturer.text?.toString()?.trim().orEmpty(),
            phone = binding.etPhone.text?.toString()?.trim().orEmpty(),
            color = course?.color ?: (name.length % 6),
            reminder = binding.switchReminder.isChecked()
        )
        val id = if (course == null) repo.db.insertCourse(item) else {
            repo.db.updateCourse(item)
            item.id
        }
        AlarmScheduler.scheduleClassReminder(context, item.copy(id = id))
        hideKeyboard(binding.root)
        dialog.dismiss()
        onSaved()
        toast(if (course == null) "Jadwal ditambahkan." else "Jadwal diperbarui.")
    }
    dialog.show()
}

// --------------------------------------------------------------- documents
fun Fragment.showDocumentEditor(
    document: Document?,
    pickedUri: Uri? = null,
    onSaved: () -> Unit
) {
    val binding = SheetDocumentEditorBinding.inflate(layoutInflater)
    val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
    dialog.setContentView(binding.root)
    dialog.expandFully()

    val repo = repository()
    val context = requireContext()
    var uri: Uri? = pickedUri ?: document?.uri?.let { runCatching { Uri.parse(it) }.getOrNull() }
    var type = document?.type ?: "module"

    binding.tvHeader.text = if (document == null) "Simpan berkas" else "Ubah berkas"
    binding.etTitle.setText(document?.title.orEmpty())
    binding.etCourse.setText(document?.course.orEmpty())
    binding.etNote.setText(document?.note.orEmpty())
    binding.segType.setItems(listOf("Modul", "Sertifikat"), if (type == "certificate") 1 else 0)
    binding.segType.setOnSegmentChanged { type = if (it == 1) "certificate" else "module" }

    fun renderFile() {
        binding.tvFile.text = uri?.let { context.displayName(it) } ?: "Pilih berkas dari perangkat"
        if (binding.etTitle.text.isNullOrBlank() && uri != null) {
            binding.etTitle.setText(context.displayName(uri!!).substringBeforeLast('.'))
        }
    }
    renderFile()

    // Re-picking is handled from the parent fragment; here the row just hints.
    binding.rowFile.setOnClickListener {
        if (uri == null) {
            toast("Tekan tombol + di daftar berkas untuk memilih file.")
        } else {
            toast("Berkas: ${context.displayName(uri!!)}")
        }
    }
    binding.btnDelete.visibility = if (document == null) View.GONE else View.VISIBLE
    binding.btnDelete.setOnClickListener {
        val target = document ?: return@setOnClickListener
        confirmDelete("berkas ini") {
            repo.db.deleteDocument(target.id)
            dialog.dismiss()
            onSaved()
            toast("Berkas dihapus dari brankas.")
        }
    }
    binding.btnSave.setOnClickListener {
        val title = binding.etTitle.text?.toString()?.trim().orEmpty()
        val chosen = uri
        if (title.isEmpty()) {
            binding.etTitle.error = "Judul berkas wajib diisi"
            binding.etTitle.requestFocus()
            return@setOnClickListener
        }
        if (chosen == null) {
            toast("Pilih berkas terlebih dahulu.")
            return@setOnClickListener
        }
        val item = Document(
            id = document?.id ?: 0,
            title = title,
            course = binding.etCourse.text?.toString()?.trim().orEmpty(),
            type = type,
            uri = chosen.toString(),
            note = binding.etNote.text?.toString()?.trim().orEmpty(),
            addedAt = document?.addedAt ?: DateUtils.now()
        )
        if (document == null) repo.db.insertDocument(item) else repo.db.updateDocument(item)
        hideKeyboard(binding.root)
        dialog.dismiss()
        onSaved()
        toast(if (document == null) "Berkas masuk ke brankas." else "Berkas diperbarui.")
    }
    dialog.show()
}

private fun hideKeyboard(view: View) {
    val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

@Suppress("unused")
private fun accentList(context: Context): List<Int> = Palette.accents(context).let {
    it.map { color -> ColorUtils.setAlphaComponent(color, 255) }
}
