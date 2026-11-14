package com.matfess.app.ui.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.databinding.FragmentScheduleBinding
import com.matfess.app.service.AlarmScheduler
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.ui.schedule.ScheduleAdapter
import com.matfess.app.util.DateUtils

class ScheduleFragment : BaseFragment() {

    private var _binding: FragmentScheduleBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ScheduleAdapter
    private var selectedDay = DateUtils.dayIndex()
    private var allCourses: List<com.matfess.app.data.Course> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScheduleBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = ScheduleAdapter(
            compact = false,
            onClick = { course -> showCourseEditor(course) { reload() } },
            onLongClick = { course -> confirmDelete(course) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.segmentedDays.setItems(
            (1..7).map { DateUtils.dayShort(it) },
            selectedDay - 1
        )
        binding.segmentedDays.setOnSegmentChanged {
            selectedDay = it + 1
            reload()
        }
        binding.btnAdd.setOnClickListener {
            showCourseEditor(null) { reload() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) reload()
    }

    private fun reload() {
        val b = _binding ?: return
        allCourses = repo.db.getCourses()
        val filtered = allCourses.filter { it.day == selectedDay }.sortedBy { it.startTime }
        adapter.submitList(filtered)
        b.recycler.isVisible = filtered.isNotEmpty()
        b.tvEmpty.isVisible = filtered.isEmpty()
        b.tvEmpty.text = if (allCourses.isEmpty()) {
            "Belum ada jadwal. Tekan tombol + untuk menambahkan mata kuliah."
        } else {
            "Tidak ada kelas pada hari ${DateUtils.dayName(selectedDay)}. Waktu untuk istirahat atau belajar mandiri."
        }
        b.tvCount.text = "${filtered.size} kelas · ${DateUtils.dayName(selectedDay)}"
    }

    private fun confirmDelete(course: com.matfess.app.data.Course) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus jadwal?")
            .setMessage("\"${course.name}\" akan dihapus dari jadwal mingguan.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repo.db.deleteCourse(course.id)
                AlarmScheduler.cancelClassReminder(requireContext(), course.id)
                reload()
                toast("Jadwal dihapus.")
            }
            .show()
    }

    override fun onDestroyView() {
        _binding?.recycler?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
