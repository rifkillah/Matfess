package com.matfess.app.data

import android.content.Context
import com.matfess.app.util.DateUtils
import java.util.Calendar

class MatfessRepository(context: Context) {

    val db: DbHelper = DbHelper(context.applicationContext)
    val prefs: Prefs = Prefs(context.applicationContext)

    data class Stats(
        val pendingTasks: Int,
        val doneTasks: Int,
        val notes: Int,
        val certificates: Int,
        val focusMinutes: Int,
        val focusSessions: Int
    )

    fun stats(): Stats = Stats(
        pendingTasks = db.countPendingTasks(),
        doneTasks = db.countDoneTasks(),
        notes = db.countNotes(),
        certificates = db.countCertificates(),
        focusMinutes = prefs.focusMinutesTotal,
        focusSessions = prefs.focusSessionsTotal
    )

    fun addFocusSession(minutes: Int) {
        prefs.focusSessionsTotal = prefs.focusSessionsTotal + 1
        prefs.focusMinutesTotal = prefs.focusMinutesTotal + minutes
    }

    /**
     * Creates a rich, believable demo account so the app can be presented
     * immediately without typing data in front of the lecturer.
     */
    fun seedDemoData() {
        if (db.getCourses().isNotEmpty() || db.getNotes().isNotEmpty()) return

        val now = DateUtils.now()
        val today = DateUtils.dayIndex(now)

        val courses = listOf(
            Course(name = "Struktur Data", day = today, startTime = "08:00", endTime = "09:40", room = "Lab Komputer 3", lecturer = "Dr. Rani Pratama", phone = "081234567890", color = 0),
            Course(name = "Basis Data Lanjut", day = today, startTime = "10:00", endTime = "11:40", room = "Ruang 2.4", lecturer = "Budi Santoso, M.Kom", phone = "081298765432", color = 1),
            Course(name = "Pemrograman Mobile", day = nextDay(today, 1), startTime = "13:00", endTime = "15:30", room = "Lab Mobile", lecturer = "Anisa Putri, M.T", phone = "081377788899", color = 2),
            Course(name = "Jaringan Komputer", day = nextDay(today, 2), startTime = "09:00", endTime = "10:40", room = "Ruang 1.2", lecturer = "Dr. Fajar Nugraha", phone = "081355566677", color = 3),
            Course(name = "Kecerdasan Buatan", day = nextDay(today, 3), startTime = "15:30", endTime = "17:00", room = "Ruang AI", lecturer = "Prof. Dimas Arya", phone = "081399900011", color = 4)
        )
        courses.forEach { db.insertCourse(it) }

        db.insertNote(
            Note(
                title = "Big-O & kompleksitas algoritma",
                content = "Insert pada linked list O(1) jika pointer ekor disimpan. Binary search O(log n) tetapi butuh data terurut. Heap sort O(n log n) dan in-place.",
                course = "Struktur Data",
                date = now,
                color = 0,
                pinned = true
            )
        )
        db.insertNote(
            Note(
                title = "Normalisasi sampai 3NF",
                content = "1NF: hilangkan atribut multinilai. 2NF: hilangkan ketergantungan parsial. 3NF: hilangkan ketergantungan transitif. Cek functional dependency sebelum memecah tabel.",
                course = "Basis Data Lanjut",
                date = now - 86_400_000L,
                color = 1
            )
        )
        db.insertNote(
            Note(
                title = "Activity Lifecycle untuk demo",
                content = "onCreate → onStart → onResume → onPause → onStop → onDestroy. Simpan state timer di onSaveInstanceState dan hentikan ticker di onPause.",
                course = "Pemrograman Mobile",
                date = now - 2 * 86_400_000L,
                color = 2
            )
        )

        val tomorrow2359 = DateUtils.combine(now + 86_400_000L, 23, 59)
        db.insertTask(Task(title = "Laporan Praktikum Stack", course = "Struktur Data", description = "Buat implementasi stack + analisis kompleksitas, kumpulkan PDF.", deadline = tomorrow2359, priority = 2, reminderMinutes = 120))
        db.insertTask(Task(title = "ERD Sistem Perpustakaan", course = "Basis Data Lanjut", description = "Normalisasi hingga 3NF dan buat relasi antar tabel.", deadline = DateUtils.combine(now + 3 * 86_400_000L, 20, 0), priority = 2, reminderMinutes = 60))
        db.insertTask(Task(title = "Kuis Jaringan Komputer", course = "Jaringan Komputer", description = "Materi subnetting, OSI layer, dan routing dasar.", deadline = DateUtils.combine(now + 5 * 86_400_000L, 9, 0), priority = 1, reminderMinutes = 30))
        db.insertTask(Task(title = "Revisi prototipe UI Matfess", course = "Pemrograman Mobile", description = "Rapikan flow onboarding dan cek kontras dark mode.", deadline = DateUtils.combine(now + 8 * 86_400_000L, 18, 0), priority = 1, reminderMinutes = 30))
        db.insertTask(Task(title = "Review paper AI", course = "Kecerdasan Buatan", description = "Ringkas 2 paper tentang computer vision.", deadline = DateUtils.combine(now - 2 * 86_400_000L, 17, 0), done = true, priority = 0, reminderMinutes = 30))

        db.insertGraduation(GraduationItem(title = "Sidang Proposal", category = "Skripsi", targetDate = now - 20 * 86_400_000L, done = true, note = "Sudah lulus dengan revisi minor."))
        db.insertGraduation(GraduationItem(title = "Skripsi & Sidang Akhir", category = "Skripsi", targetDate = now + 110 * 86_400_000L, note = "Target bab 1–3 selesai bulan ini."))
        db.insertGraduation(GraduationItem(title = "Bebas Pustaka", category = "Administrasi", targetDate = now + 130 * 86_400_000L))
        db.insertGraduation(GraduationItem(title = "Sertifikat TOEIC", category = "Sertifikasi", targetDate = now + 60 * 86_400_000L, note = "Minimal skor 550."))
        db.insertGraduation(GraduationItem(title = "KKN / Magang", category = "Pengalaman", targetDate = now + 80 * 86_400_000L))

        db.insertRevision(Revision(course = "Kalkulus II", grade = "C", targetSemester = "Semester 5", note = "Fokus integral lipat dan deret."))
        db.insertRevision(Revision(course = "Fisika Dasar", grade = "D", targetSemester = "Semester 5", note = "Ulang agar tidak menghambat prasyarat."))

        db.insertEvent(CampusEvent(title = "Batas Pengisian KRS", date = DateUtils.combine(now + 4 * 86_400_000L, 23, 59), location = "Portal Akademik", note = "Pastikan semua mata kuliah sudah disetujui PA.", type = "krs"))
        db.insertEvent(CampusEvent(title = "Seminar Nasional AI", date = DateUtils.combine(now + 9 * 86_400_000L, 9, 0), location = "Auditorium Utama", note = "Bawa laptop dan kartu mahasiswa.", type = "kampus"))
        db.insertEvent(CampusEvent(title = "Pembayaran UKT", date = DateUtils.combine(now + 14 * 86_400_000L, 23, 59), location = "Bank / Virtual Account", note = "Simpan bukti pembayaran.", type = "ukt"))

        val start = DateUtils.startOfDay(now - 42 * 86_400_000L)
        prefs.semesterStart = start
        prefs.semesterEnd = start + 140 * 86_400_000L
        prefs.utsStart = DateUtils.startOfDay(now + 14 * 86_400_000L)
        prefs.utsEnd = DateUtils.startOfDay(now + 19 * 86_400_000L)
        prefs.uasStart = DateUtils.startOfDay(now + 75 * 86_400_000L)
        prefs.uasEnd = DateUtils.startOfDay(now + 80 * 86_400_000L)
        prefs.focusMinutesTotal = 340
        prefs.focusSessionsTotal = 14
    }

    private fun nextDay(day: Int, plus: Int): Int {
        val result = day + plus
        return if (result > 7) result - 7 else result
    }

    @Suppress("unused")
    private fun startOfToday(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
