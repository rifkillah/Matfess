package com.matfess.app.data

data class User(
    val id: Long,
    val name: String,
    val nim: String,
    val major: String,
    val semester: Int,
    val password: String
)

data class Course(
    val id: Long = 0,
    val name: String,
    val day: Int,              // 1 = Monday ... 7 = Sunday
    val startTime: String,     // HH:mm
    val endTime: String,       // HH:mm
    val room: String,
    val lecturer: String,
    val phone: String,
    val color: Int = 0,
    val reminder: Boolean = true
)

data class Note(
    val id: Long = 0,
    val title: String,
    val content: String,
    val course: String,
    val date: Long,
    val color: Int = 0,
    val pinned: Boolean = false
)

data class Task(
    val id: Long = 0,
    val title: String,
    val course: String,
    val description: String,
    val deadline: Long,
    val done: Boolean = false,
    val priority: Int = 1,      // 0 low, 1 medium, 2 high
    val reminderMinutes: Int = 30
)

data class Document(
    val id: Long = 0,
    val title: String,
    val course: String,
    val type: String,           // module / certificate
    val uri: String,
    val note: String,
    val addedAt: Long
)

data class GraduationItem(
    val id: Long = 0,
    val title: String,
    val category: String,
    val targetDate: Long,
    val done: Boolean = false,
    val note: String = ""
)

data class Revision(
    val id: Long = 0,
    val course: String,
    val grade: String,
    val targetSemester: String,
    val note: String = ""
)

data class CampusEvent(
    val id: Long = 0,
    val title: String,
    val date: Long,
    val location: String,
    val note: String,
    val type: String            // kampus / ukt / krs / lainnya
)
