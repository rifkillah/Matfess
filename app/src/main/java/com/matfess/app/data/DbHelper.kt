package com.matfess.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Offline-first SQLite layer. Every feature in Matfess reads and writes here,
 * so the app remains fully usable without an internet connection.
 */
class DbHelper(context: Context) : SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {

    companion object {
        private const val DB_NAME = "matfess.db"
        private const val DB_VERSION = 1

        const val T_USERS = "users"
        const val T_COURSES = "courses"
        const val T_NOTES = "notes"
        const val T_TASKS = "tasks"
        const val T_DOCS = "documents"
        const val T_GRADUATION = "graduation_items"
        const val T_REVISIONS = "revisions"
        const val T_EVENTS = "events"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $T_USERS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                nim TEXT NOT NULL UNIQUE,
                major TEXT NOT NULL,
                semester INTEGER NOT NULL DEFAULT 1,
                password TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_COURSES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                day INTEGER NOT NULL,
                start_time TEXT NOT NULL,
                end_time TEXT NOT NULL,
                room TEXT,
                lecturer TEXT,
                phone TEXT,
                color INTEGER NOT NULL DEFAULT 0,
                reminder INTEGER NOT NULL DEFAULT 1
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_NOTES (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                content TEXT,
                course TEXT,
                date INTEGER NOT NULL,
                color INTEGER NOT NULL DEFAULT 0,
                pinned INTEGER NOT NULL DEFAULT 0
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_TASKS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                course TEXT,
                description TEXT,
                deadline INTEGER NOT NULL,
                done INTEGER NOT NULL DEFAULT 0,
                priority INTEGER NOT NULL DEFAULT 1,
                reminder_minutes INTEGER NOT NULL DEFAULT 30
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_DOCS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                course TEXT,
                type TEXT NOT NULL,
                uri TEXT NOT NULL,
                note TEXT,
                added_at INTEGER NOT NULL
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_GRADUATION (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                category TEXT,
                target_date INTEGER NOT NULL,
                done INTEGER NOT NULL DEFAULT 0,
                note TEXT
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_REVISIONS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                course TEXT NOT NULL,
                grade TEXT NOT NULL,
                target_semester TEXT,
                note TEXT
            )"""
        )
        db.execSQL(
            """CREATE TABLE $T_EVENTS (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                title TEXT NOT NULL,
                date INTEGER NOT NULL,
                location TEXT,
                note TEXT,
                type TEXT NOT NULL DEFAULT 'kampus'
            )"""
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Early academic prototype: rebuild cleanly when the schema evolves.
        listOf(T_USERS, T_COURSES, T_NOTES, T_TASKS, T_DOCS, T_GRADUATION, T_REVISIONS, T_EVENTS)
            .forEach { db.execSQL("DROP TABLE IF EXISTS $it") }
        onCreate(db)
    }

    // ---------------------------------------------------------------- users
    fun registerUser(name: String, nim: String, major: String, semester: Int, password: String): Long {
        val values = ContentValues().apply {
            put("name", name)
            put("nim", nim)
            put("major", major)
            put("semester", semester)
            put("password", password)
            put("created_at", System.currentTimeMillis())
        }
        return writableDatabase.insert(T_USERS, null, values)
    }

    fun findUser(nim: String, password: String): User? {
        readableDatabase.query(
            T_USERS, null, "nim = ? AND password = ?", arrayOf(nim, password),
            null, null, null, "1"
        ).use { c ->
            return if (c.moveToFirst()) c.toUser() else null
        }
    }

    fun findUserByNim(nim: String): User? {
        readableDatabase.query(
            T_USERS, null, "nim = ?", arrayOf(nim), null, null, null, "1"
        ).use { c ->
            return if (c.moveToFirst()) c.toUser() else null
        }
    }

    fun getUser(id: Long): User? {
        readableDatabase.query(
            T_USERS, null, "id = ?", arrayOf(id.toString()), null, null, null, "1"
        ).use { c ->
            return if (c.moveToFirst()) c.toUser() else null
        }
    }

    fun updateUser(id: Long, name: String, major: String, semester: Int): Int {
        val values = ContentValues().apply {
            put("name", name)
            put("major", major)
            put("semester", semester)
        }
        return writableDatabase.update(T_USERS, values, "id = ?", arrayOf(id.toString()))
    }

    private fun Cursor.toUser() = User(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        nim = getString(getColumnIndexOrThrow("nim")),
        major = getString(getColumnIndexOrThrow("major")),
        semester = getInt(getColumnIndexOrThrow("semester")),
        password = getString(getColumnIndexOrThrow("password"))
    )

    // -------------------------------------------------------------- courses
    fun insertCourse(course: Course): Long = writableDatabase.insert(T_COURSES, null, course.values())

    fun updateCourse(course: Course): Int = writableDatabase.update(
        T_COURSES, course.values(), "id = ?", arrayOf(course.id.toString())
    )

    fun deleteCourse(id: Long): Int =
        writableDatabase.delete(T_COURSES, "id = ?", arrayOf(id.toString()))

    fun getCourses(): List<Course> {
        val list = mutableListOf<Course>()
        readableDatabase.query(T_COURSES, null, null, null, null, null, "day ASC, start_time ASC").use { c ->
            while (c.moveToNext()) list.add(c.toCourse())
        }
        return list
    }

    fun getCoursesForDay(day: Int): List<Course> =
        getCourses().filter { it.day == day }.sortedBy { it.startTime }

    private fun Course.values() = ContentValues().apply {
        put("name", name)
        put("day", day)
        put("start_time", startTime)
        put("end_time", endTime)
        put("room", room)
        put("lecturer", lecturer)
        put("phone", phone)
        put("color", color)
        put("reminder", if (reminder) 1 else 0)
    }

    private fun Cursor.toCourse() = Course(
        id = getLong(getColumnIndexOrThrow("id")),
        name = getString(getColumnIndexOrThrow("name")),
        day = getInt(getColumnIndexOrThrow("day")),
        startTime = getString(getColumnIndexOrThrow("start_time")),
        endTime = getString(getColumnIndexOrThrow("end_time")),
        room = getString(getColumnIndexOrThrow("room")) ?: "",
        lecturer = getString(getColumnIndexOrThrow("lecturer")) ?: "",
        phone = getString(getColumnIndexOrThrow("phone")) ?: "",
        color = getInt(getColumnIndexOrThrow("color")),
        reminder = getInt(getColumnIndexOrThrow("reminder")) == 1
    )

    // ---------------------------------------------------------------- notes
    fun insertNote(note: Note): Long = writableDatabase.insert(T_NOTES, null, note.values())

    fun updateNote(note: Note): Int = writableDatabase.update(
        T_NOTES, note.values(), "id = ?", arrayOf(note.id.toString())
    )

    fun deleteNote(id: Long): Int =
        writableDatabase.delete(T_NOTES, "id = ?", arrayOf(id.toString()))

    fun toggleNotePin(id: Long, pinned: Boolean): Int {
        val values = ContentValues().apply { put("pinned", if (pinned) 1 else 0) }
        return writableDatabase.update(T_NOTES, values, "id = ?", arrayOf(id.toString()))
    }

    fun getNotes(): List<Note> {
        val list = mutableListOf<Note>()
        readableDatabase.query(T_NOTES, null, null, null, null, null, "pinned DESC, date DESC").use { c ->
            while (c.moveToNext()) list.add(c.toNote())
        }
        return list
    }

    fun getNote(id: Long): Note? {
        readableDatabase.query(T_NOTES, null, "id = ?", arrayOf(id.toString()), null, null, null, "1").use { c ->
            return if (c.moveToFirst()) c.toNote() else null
        }
    }

    fun countNotes(): Int = count(T_NOTES)

    private fun Note.values() = ContentValues().apply {
        put("title", title)
        put("content", content)
        put("course", course)
        put("date", date)
        put("color", color)
        put("pinned", if (pinned) 1 else 0)
    }

    private fun Cursor.toNote() = Note(
        id = getLong(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        content = getString(getColumnIndexOrThrow("content")) ?: "",
        course = getString(getColumnIndexOrThrow("course")) ?: "",
        date = getLong(getColumnIndexOrThrow("date")),
        color = getInt(getColumnIndexOrThrow("color")),
        pinned = getInt(getColumnIndexOrThrow("pinned")) == 1
    )

    // ---------------------------------------------------------------- tasks
    fun insertTask(task: Task): Long = writableDatabase.insert(T_TASKS, null, task.values())

    fun updateTask(task: Task): Int = writableDatabase.update(
        T_TASKS, task.values(), "id = ?", arrayOf(task.id.toString())
    )

    fun deleteTask(id: Long): Int =
        writableDatabase.delete(T_TASKS, "id = ?", arrayOf(id.toString()))

    fun setTaskDone(id: Long, done: Boolean): Int {
        val values = ContentValues().apply { put("done", if (done) 1 else 0) }
        return writableDatabase.update(T_TASKS, values, "id = ?", arrayOf(id.toString()))
    }

    fun getTasks(): List<Task> {
        val list = mutableListOf<Task>()
        readableDatabase.query(T_TASKS, null, null, null, null, null, "done ASC, deadline ASC").use { c ->
            while (c.moveToNext()) list.add(c.toTask())
        }
        return list
    }

    fun getTask(id: Long): Task? {
        readableDatabase.query(T_TASKS, null, "id = ?", arrayOf(id.toString()), null, null, null, "1").use { c ->
            return if (c.moveToFirst()) c.toTask() else null
        }
    }

    fun getUpcomingTasks(): List<Task> = getTasks().filter { !it.done && it.deadline > System.currentTimeMillis() }

    fun countPendingTasks(): Int {
        readableDatabase.rawQuery(
            "SELECT COUNT(*) FROM $T_TASKS WHERE done = 0 AND deadline > ?",
            arrayOf(System.currentTimeMillis().toString())
        ).use { c -> return if (c.moveToFirst()) c.getInt(0) else 0 }
    }

    fun countDoneTasks(): Int {
        readableDatabase.rawQuery("SELECT COUNT(*) FROM $T_TASKS WHERE done = 1", null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }

    private fun Task.values() = ContentValues().apply {
        put("title", title)
        put("course", course)
        put("description", description)
        put("deadline", deadline)
        put("done", if (done) 1 else 0)
        put("priority", priority)
        put("reminder_minutes", reminderMinutes)
    }

    private fun Cursor.toTask() = Task(
        id = getLong(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        course = getString(getColumnIndexOrThrow("course")) ?: "",
        description = getString(getColumnIndexOrThrow("description")) ?: "",
        deadline = getLong(getColumnIndexOrThrow("deadline")),
        done = getInt(getColumnIndexOrThrow("done")) == 1,
        priority = getInt(getColumnIndexOrThrow("priority")),
        reminderMinutes = getInt(getColumnIndexOrThrow("reminder_minutes"))
    )

    // ------------------------------------------------------------ documents
    fun insertDocument(document: Document): Long =
        writableDatabase.insert(T_DOCS, null, document.values())

    fun updateDocument(document: Document): Int = writableDatabase.update(
        T_DOCS, document.values(), "id = ?", arrayOf(document.id.toString())
    )

    fun deleteDocument(id: Long): Int =
        writableDatabase.delete(T_DOCS, "id = ?", arrayOf(id.toString()))

    fun getDocuments(): List<Document> {
        val list = mutableListOf<Document>()
        readableDatabase.query(T_DOCS, null, null, null, null, null, "added_at DESC").use { c ->
            while (c.moveToNext()) list.add(c.toDocument())
        }
        return list
    }

    fun getDocument(id: Long): Document? {
        readableDatabase.query(T_DOCS, null, "id = ?", arrayOf(id.toString()), null, null, null, "1").use { c ->
            return if (c.moveToFirst()) c.toDocument() else null
        }
    }

    fun countCertificates(): Int = count(T_DOCS, "type = ?", arrayOf("certificate"))

    private fun Document.values() = ContentValues().apply {
        put("title", title)
        put("course", course)
        put("type", type)
        put("uri", uri)
        put("note", note)
        put("added_at", addedAt)
    }

    private fun Cursor.toDocument() = Document(
        id = getLong(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        course = getString(getColumnIndexOrThrow("course")) ?: "",
        type = getString(getColumnIndexOrThrow("type")),
        uri = getString(getColumnIndexOrThrow("uri")),
        note = getString(getColumnIndexOrThrow("note")) ?: "",
        addedAt = getLong(getColumnIndexOrThrow("added_at"))
    )

    // ----------------------------------------------------------- graduation
    fun insertGraduation(item: GraduationItem): Long =
        writableDatabase.insert(T_GRADUATION, null, item.values())

    fun updateGraduation(item: GraduationItem): Int = writableDatabase.update(
        T_GRADUATION, item.values(), "id = ?", arrayOf(item.id.toString())
    )

    fun deleteGraduation(id: Long): Int =
        writableDatabase.delete(T_GRADUATION, "id = ?", arrayOf(id.toString()))

    fun setGraduationDone(id: Long, done: Boolean): Int {
        val values = ContentValues().apply { put("done", if (done) 1 else 0) }
        return writableDatabase.update(T_GRADUATION, values, "id = ?", arrayOf(id.toString()))
    }

    fun getGraduationItems(): List<GraduationItem> {
        val list = mutableListOf<GraduationItem>()
        readableDatabase.query(T_GRADUATION, null, null, null, null, null, "done ASC, target_date ASC").use { c ->
            while (c.moveToNext()) list.add(c.toGraduation())
        }
        return list
    }

    private fun GraduationItem.values() = ContentValues().apply {
        put("title", title)
        put("category", category)
        put("target_date", targetDate)
        put("done", if (done) 1 else 0)
        put("note", note)
    }

    private fun Cursor.toGraduation() = GraduationItem(
        id = getLong(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        category = getString(getColumnIndexOrThrow("category")) ?: "Lainnya",
        targetDate = getLong(getColumnIndexOrThrow("target_date")),
        done = getInt(getColumnIndexOrThrow("done")) == 1,
        note = getString(getColumnIndexOrThrow("note")) ?: ""
    )

    // ------------------------------------------------------------ revisions
    fun insertRevision(revision: Revision): Long =
        writableDatabase.insert(T_REVISIONS, null, revision.values())

    fun updateRevision(revision: Revision): Int = writableDatabase.update(
        T_REVISIONS, revision.values(), "id = ?", arrayOf(revision.id.toString())
    )

    fun deleteRevision(id: Long): Int =
        writableDatabase.delete(T_REVISIONS, "id = ?", arrayOf(id.toString()))

    fun getRevisions(): List<Revision> {
        val list = mutableListOf<Revision>()
        readableDatabase.query(T_REVISIONS, null, null, null, null, null, "course ASC").use { c ->
            while (c.moveToNext()) list.add(c.toRevision())
        }
        return list
    }

    private fun Revision.values() = ContentValues().apply {
        put("course", course)
        put("grade", grade)
        put("target_semester", targetSemester)
        put("note", note)
    }

    private fun Cursor.toRevision() = Revision(
        id = getLong(getColumnIndexOrThrow("id")),
        course = getString(getColumnIndexOrThrow("course")),
        grade = getString(getColumnIndexOrThrow("grade")),
        targetSemester = getString(getColumnIndexOrThrow("target_semester")) ?: "",
        note = getString(getColumnIndexOrThrow("note")) ?: ""
    )

    // --------------------------------------------------------------- events
    fun insertEvent(event: CampusEvent): Long = writableDatabase.insert(T_EVENTS, null, event.values())

    fun updateEvent(event: CampusEvent): Int = writableDatabase.update(
        T_EVENTS, event.values(), "id = ?", arrayOf(event.id.toString())
    )

    fun deleteEvent(id: Long): Int =
        writableDatabase.delete(T_EVENTS, "id = ?", arrayOf(id.toString()))

    fun getEvents(): List<CampusEvent> {
        val list = mutableListOf<CampusEvent>()
        readableDatabase.query(T_EVENTS, null, null, null, null, null, "date ASC").use { c ->
            while (c.moveToNext()) list.add(c.toEvent())
        }
        return list
    }

    private fun CampusEvent.values() = ContentValues().apply {
        put("title", title)
        put("date", date)
        put("location", location)
        put("note", note)
        put("type", type)
    }

    private fun Cursor.toEvent() = CampusEvent(
        id = getLong(getColumnIndexOrThrow("id")),
        title = getString(getColumnIndexOrThrow("title")),
        date = getLong(getColumnIndexOrThrow("date")),
        location = getString(getColumnIndexOrThrow("location")) ?: "",
        note = getString(getColumnIndexOrThrow("note")) ?: "",
        type = getString(getColumnIndexOrThrow("type")) ?: "kampus"
    )

    private fun count(table: String, selection: String? = null, args: Array<String>? = null): Int {
        readableDatabase.query(table, arrayOf("COUNT(*)"), selection, args, null, null, null).use { c ->
            return if (c.moveToFirst()) c.getInt(0) else 0
        }
    }
}
