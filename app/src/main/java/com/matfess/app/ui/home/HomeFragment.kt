package com.matfess.app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.matfess.app.R
import com.matfess.app.data.Course
import com.matfess.app.data.Task
import com.matfess.app.databinding.FragmentHomeBinding
import com.matfess.app.network.Quote
import com.matfess.app.network.RemoteRepository
import com.matfess.app.service.AlarmScheduler
import com.matfess.app.service.DndHelper
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.ui.schedule.ScheduleAdapter
import com.matfess.app.ui.tasks.TaskAdapter
import com.matfess.app.util.DateUtils
import com.matfess.app.util.expandFully
import com.matfess.app.util.Palette
import com.matfess.app.util.applyTopInset
import com.matfess.app.util.entrance
import com.matfess.app.util.pickDate
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var scheduleAdapter: ScheduleAdapter
    private lateinit var taskAdapter: TaskAdapter

    private val ticker = object : Runnable {
        override fun run() {
            _binding?.let { updateCountdown() }
            _binding?.scroll?.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scroll.applyTopInset()

        scheduleAdapter = ScheduleAdapter(
            compact = true,
            onClick = { openAcademic(page = 1) },
            onLongClick = { openAcademic(page = 1) }
        )
        taskAdapter = TaskAdapter(
            onClick = { openAcademic(page = 2) },
            onToggle = { task, done -> toggleTask(task, done) },
            onLongClick = { openAcademic(page = 2) }
        )
        binding.rvToday.layoutManager = LinearLayoutManager(requireContext())
        binding.rvToday.adapter = scheduleAdapter
        binding.rvToday.itemAnimator = null
        binding.rvTasks.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTasks.adapter = taskAdapter
        binding.rvTasks.itemAnimator = null

        // RecyclerView is focusable by default (it overrides the XML attribute),
        // so on a cold start the window hands focus to the bottom list and the
        // scroll container jumps down to reveal it. Keep focus on the page itself
        // and start at the top on first creation.
        binding.scroll.isFocusableInTouchMode = true
        binding.scroll.requestFocus()
        if (savedInstanceState == null) {
            binding.scroll.post { binding.scroll.scrollTo(0, 0) }
        }

        binding.btnSettings.setOnClickListener { openTab(R.id.profileFragment) }
        binding.cardFocus.setOnClickListener { openTab(R.id.focusFragment) }
        binding.cardDayTracker.setOnClickListener { showSemesterSettings() }
        binding.btnEditExam.setOnClickListener { showExamSettings() }
        binding.btnSeeSchedule.setOnClickListener { openAcademic(page = 1) }
        binding.btnSeeTasks.setOnClickListener { openAcademic(page = 2) }
        binding.quickNote.setOnClickListener { openAcademic(page = 0) }
        binding.quickTask.setOnClickListener { openAcademic(page = 2) }
        binding.quickSchedule.setOnClickListener { openAcademic(page = 1) }
        binding.quickRandom.setOnClickListener { openTab(R.id.randomGroupFragment) }
        binding.btnRefreshQuote.setOnClickListener { loadQuote(force = true) }
        binding.quoteCard.setOnClickListener { loadQuote(force = true) }

        binding.switchDnd.setChecked(DndHelper.isEnabled(requireContext()), notify = false, animate = false)
        binding.switchDnd.setOnCheckedChange { enabled ->
            val applied = DndHelper.setEnabled(requireContext(), enabled)
            if (!applied && enabled) {
                binding.switchDnd.setChecked(false, notify = false)
                toast("Izinkan akses Jangan Ganggu dulu, lalu coba lagi.")
            }
        }

        bindProfile()
        loadQuote(force = false)
        animateEntrance()
    }

    override fun onResume() {
        super.onResume()
        if (_binding == null) return
        refreshAll()
        _binding?.scroll?.removeCallbacks(ticker)
        _binding?.scroll?.post(ticker)
    }

    override fun onPause() {
        _binding?.scroll?.removeCallbacks(ticker)
        super.onPause()
    }

    override fun onDestroyView() {
        _binding?.scroll?.removeCallbacks(ticker)
        _binding?.rvToday?.adapter = null
        _binding?.rvTasks?.adapter = null
        _binding = null
        super.onDestroyView()
    }

    // --------------------------------------------------------------- binding
    private fun bindProfile() {
        binding.tvGreeting.text = DateUtils.greeting()
        binding.tvName.text = prefs.userName
        binding.tvDate.text = DateUtils.formatFull(DateUtils.now())
            .replaceFirstChar { it.uppercase() }
        binding.tvAvatar.text = initials(prefs.userName)
        binding.avatar.backgroundTintList = android.content.res.ColorStateList.valueOf(
            Palette.initialColor(requireContext(), prefs.userName)
        )
        binding.switchDnd.setChecked(DndHelper.isEnabled(requireContext()), notify = false, animate = false)
    }

    private fun refreshAll() {
        if (_binding == null) return
        bindProfile()
        updateCountdown()
        updateTimeline()
        updateStats()
        updateLists()
    }

    private fun updateCountdown() {
        val b = _binding ?: return
        val phase = DateUtils.examPhase(prefs)
        val countdown = DateUtils.countdown(phase.targetMillis)

        b.tvExamPhase.text = when (phase.phase) {
            "PRE_UTS" -> "MENUJU UTS"
            "UTS" -> "MASA UTS"
            "PRE_UAS" -> "MENUJU UAS"
            "UAS" -> "MASA UAS"
            else -> "SEMESTER SELESAI"
        }
        val accent = when (phase.phase) {
            "UTS", "UAS" -> ContextCompat.getColor(requireContext(), R.color.accent_coral)
            "PRE_UAS" -> ContextCompat.getColor(requireContext(), R.color.accent_violet)
            else -> ContextCompat.getColor(requireContext(), R.color.accent_primary)
        }
        b.tvExamPhase.setTextColor(accent)
        b.tvExamPhase.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 30))
        b.tvSeconds.setTextColor(accent)
        b.examProgress.setIndicatorColor(accent)

        b.tvExamTitle.text = phase.title
        b.tvExamSubtitle.text = phase.subtitle
        b.tvDays.text = countdown.days.toString()
        b.tvHours.text = countdown.hours.toString().padStart(2, '0')
        b.tvMinutes.text = countdown.minutes.toString().padStart(2, '0')
        b.tvSeconds.text = countdown.seconds.toString().padStart(2, '0')
        b.examProgress.setProgressCompat((phase.progress * 100).toInt(), true)
        b.tvExamTarget.text = "Target ${DateUtils.formatFull(phase.targetMillis)}"
        b.tvExamPercent.text = "${(phase.progress * 100).toInt()}%"
    }

    private fun updateTimeline() {
        val b = _binding ?: return
        val day = DateUtils.semesterDay(prefs)
        val total = (DateUtils.daysBetween(prefs.semesterStart, prefs.semesterEnd))
            .toInt().coerceAtLeast(1)
        b.tvDayTracker.text = "Hari ke-$day"
        b.tvDayTrackerSub.text = "dari $total hari semester"
        b.ringSemester.setProgress(DateUtils.semesterProgress(prefs))
    }

    private fun updateStats() {
        val b = _binding ?: return
        val stats = repo.stats()
        b.tvStatTasks.text = stats.pendingTasks.toString()
        b.tvStatNotes.text = stats.notes.toString()
        b.tvStatCerts.text = stats.certificates.toString()
        b.tvFocusMinutes.text = stats.focusMinutes.toString()
        b.tvFocusSessions.text = if (stats.focusSessions == 0) {
            "Mulai sesi pertamamu"
        } else {
            "${stats.focusSessions} sesi selesai"
        }
    }

    private fun updateLists() {
        val b = _binding ?: return
        val today = repo.db.getCoursesForDay(DateUtils.dayIndex())
        scheduleAdapter.submitList(today)
        b.tvEmptySchedule.isVisible = today.isEmpty()
        b.rvToday.isVisible = today.isNotEmpty()

        val tasks = repo.db.getUpcomingTasks().take(3)
        taskAdapter.submitList(tasks)
        b.tvEmptyTasks.isVisible = tasks.isEmpty()
        b.rvTasks.isVisible = tasks.isNotEmpty()
    }

    private fun animateEntrance() {
        binding.examCard.entrance(40)
        binding.cardDayTracker.entrance(110)
        binding.cardFocus.entrance(160)
        binding.quoteCard.entrance(220)
    }

    // ---------------------------------------------------------------- actions
    private fun toggleTask(task: Task, done: Boolean) {
        repo.db.setTaskDone(task.id, done)
        if (done) {
            AlarmScheduler.cancelTask(requireContext(), task.id)
            toast("Mantap, satu tugas beres!")
        } else {
            AlarmScheduler.scheduleTask(requireContext(), task.copy(done = false))
        }
        updateStats()
        updateLists()
    }

    private fun loadQuote(force: Boolean) {
        val b = _binding ?: return
        if (prefs.quoteText.isNotBlank()) {
            b.tvQuote.text = "\u201C${prefs.quoteText}\u201D"
            b.tvQuoteAuthor.text = if (prefs.quoteAuthor.isBlank()) "Matfess" else "— ${prefs.quoteAuthor}"
        } else {
            b.tvQuote.text = "Memuat kutipan untuk hari ini…"
        }
        val stale = System.currentTimeMillis() - prefs.lastQuoteFetch > 6 * 60 * 60 * 1000L
        if (!force && !stale && prefs.quoteText.isNotBlank()) return

        viewLifecycleOwner.lifecycleScope.launch {
            val quote: Quote = RemoteRepository.quote()
            prefs.quoteText = quote.text
            prefs.quoteAuthor = quote.author
            prefs.lastQuoteFetch = System.currentTimeMillis()
            _binding?.let {
                it.tvQuote.alpha = 0f
                it.tvQuote.text = "\u201C${quote.text}\u201D"
                it.tvQuoteAuthor.text = if (quote.author.isBlank()) "Matfess" else "— ${quote.author}"
                it.tvQuote.animate().alpha(1f).setDuration(400).start()
            }
        }
    }

    private fun openAcademic(page: Int) {
        findNavController().navigate(
            R.id.academicFragment,
            Bundle().apply { putInt("page", page) }
        )
    }

    private fun openTab(id: Int) {
        try {
            findNavController().navigate(id)
        } catch (_: IllegalArgumentException) {
            // Already on that destination.
        }
    }

    // ------------------------------------------------------------ bottom sheets
    private fun showExamSettings() {
        val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
        val sheet = layoutInflater.inflate(R.layout.sheet_exam_settings, null)
        dialog.setContentView(sheet)
        dialog.expandFully()

        var utsStart = prefs.utsStart
        var utsEnd = prefs.utsEnd
        var uasStart = prefs.uasStart
        var uasEnd = prefs.uasEnd

        val tvUtsStart = sheet.findViewById<android.widget.TextView>(R.id.tvUtsStart)
        val tvUtsEnd = sheet.findViewById<android.widget.TextView>(R.id.tvUtsEnd)
        val tvUasStart = sheet.findViewById<android.widget.TextView>(R.id.tvUasStart)
        val tvUasEnd = sheet.findViewById<android.widget.TextView>(R.id.tvUasEnd)
        val tvPreview = sheet.findViewById<android.widget.TextView>(R.id.tvPreview)

        fun render() {
            tvUtsStart.text = DateUtils.formatFull(utsStart)
            tvUtsEnd.text = DateUtils.formatFull(utsEnd)
            tvUasStart.text = DateUtils.formatFull(uasStart)
            tvUasEnd.text = DateUtils.formatFull(uasEnd)
            val utsDays = DateUtils.daysBetween(DateUtils.now(), utsStart)
            val uasDays = DateUtils.daysBetween(DateUtils.now(), uasStart)
            tvPreview.text = buildString {
                append("UTS dalam ")
                append(if (utsDays >= 0) "$utsDays hari" else "masa ujian")
                append(" · UAS dalam ")
                append(if (uasDays >= 0) "$uasDays hari" else "masa ujian")
            }
        }
        render()

        sheet.findViewById<View>(R.id.rowUtsStart).setOnClickListener {
            pickDate(utsStart, "Tanggal mulai UTS") { utsStart = it; render() }
        }
        sheet.findViewById<View>(R.id.rowUtsEnd).setOnClickListener {
            pickDate(utsEnd, "Tanggal selesai UTS") { utsEnd = it; render() }
        }
        sheet.findViewById<View>(R.id.rowUasStart).setOnClickListener {
            pickDate(uasStart, "Tanggal mulai UAS") { uasStart = it; render() }
        }
        sheet.findViewById<View>(R.id.rowUasEnd).setOnClickListener {
            pickDate(uasEnd, "Tanggal selesai UAS") { uasEnd = it; render() }
        }
        sheet.findViewById<View>(R.id.btnReset).setOnClickListener {
            utsStart = prefs.semesterStart + 56L * DateUtils.DAY_MILLIS
            utsEnd = utsStart + 6L * DateUtils.DAY_MILLIS
            uasStart = prefs.semesterStart + 112L * DateUtils.DAY_MILLIS
            uasEnd = uasStart + 6L * DateUtils.DAY_MILLIS
            render()
            toast("Jadwal ujian dikembalikan ke pola umum.")
        }
        sheet.findViewById<View>(R.id.btnSave).setOnClickListener {
            prefs.utsStart = DateUtils.startOfDay(utsStart)
            prefs.utsEnd = DateUtils.startOfDay(utsEnd)
            prefs.uasStart = DateUtils.startOfDay(uasStart)
            prefs.uasEnd = DateUtils.startOfDay(uasEnd)
            dialog.dismiss()
            refreshAll()
            toast("Jadwal ujian diperbarui.")
        }
        dialog.show()
    }

    private fun showSemesterSettings() {
        val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
        val sheet = layoutInflater.inflate(R.layout.sheet_semester_settings, null)
        dialog.setContentView(sheet)
        dialog.expandFully()

        var start = prefs.semesterStart
        var end = prefs.semesterEnd
        val tvStart = sheet.findViewById<android.widget.TextView>(R.id.tvStart)
        val tvEnd = sheet.findViewById<android.widget.TextView>(R.id.tvEnd)
        val tvPreview = sheet.findViewById<android.widget.TextView>(R.id.tvPreview)

        fun render() {
            tvStart.text = DateUtils.formatFull(start)
            tvEnd.text = DateUtils.formatFull(end)
            val total = DateUtils.daysBetween(start, end).toInt()
            val day = DateUtils.semesterDay(prefs).coerceAtMost(total)
            tvPreview.text = "Hari ke-$day dari $total hari · ${(DateUtils.semesterProgress(prefs) * 100).toInt()}% perjalanan semester"
        }
        render()

        sheet.findViewById<View>(R.id.rowStart).setOnClickListener {
            pickDate(start, "Hari pertama semester") { start = it; render() }
        }
        sheet.findViewById<View>(R.id.rowEnd).setOnClickListener {
            pickDate(end, "Hari terakhir semester") { end = it; render() }
        }
        sheet.findViewById<View>(R.id.btnCancel).setOnClickListener { dialog.dismiss() }
        sheet.findViewById<View>(R.id.btnSave).setOnClickListener {
            if (end <= start) {
                toast("Tanggal akhir harus setelah tanggal mulai.")
                return@setOnClickListener
            }
            prefs.semesterStart = DateUtils.startOfDay(start)
            prefs.semesterEnd = DateUtils.startOfDay(end)
            dialog.dismiss()
            refreshAll()
            toast("Penghitung hari semester diperbarui.")
        }
        dialog.show()
    }

    private fun initials(name: String): String {
        val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "M"
            parts.size == 1 -> parts.first().take(2).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }

    @Suppress("unused")
    private fun todayStart(): Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Suppress("unused")
    private fun courseSubtitle(course: Course): String = buildString {
        append(course.startTime)
        append("–")
        append(course.endTime)
        if (course.room.isNotBlank()) append(" · ${course.room}")
    }
}
