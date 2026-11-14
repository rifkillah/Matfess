package com.matfess.app.ui.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.databinding.FragmentTasksBinding
import com.matfess.app.service.AlarmScheduler
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.ui.tasks.TaskAdapter
import com.matfess.app.util.DateUtils

class TasksFragment : BaseFragment() {

    private var _binding: FragmentTasksBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: TaskAdapter
    private var filter = 0 // 0 aktif, 1 selesai, 2 semua
    private var allTasks: List<com.matfess.app.data.Task> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTasksBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = TaskAdapter(
            onClick = { task -> showTaskEditor(task) { reload() } },
            onToggle = { task, done -> setDone(task, done) },
            onLongClick = { task -> confirmDelete(task) }
        )
        binding.recycler.layoutManager = LinearLayoutManager(requireContext())
        binding.recycler.adapter = adapter
        binding.segmentedFilters.setItems(listOf("Aktif", "Selesai", "Semua"), 0)
        binding.segmentedFilters.setOnSegmentChanged {
            filter = it
            reload()
        }
        binding.btnAdd.setOnClickListener {
            showTaskEditor(null) { reload() }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) reload()
    }

    private fun reload() {
        val b = _binding ?: return
        allTasks = repo.db.getTasks()
        val now = DateUtils.now()
        val filtered = when (filter) {
            1 -> allTasks.filter { it.done }.sortedByDescending { it.deadline }
            2 -> allTasks
            else -> allTasks.filter { !it.done }.sortedBy { it.deadline }
        }
        adapter.submitList(filtered)
        val active = allTasks.count { !it.done }
        val overdue = allTasks.count { !it.done && it.deadline < now }
        b.recycler.isVisible = filtered.isNotEmpty()
        b.tvEmpty.isVisible = filtered.isEmpty()
        b.tvEmpty.text = when {
            allTasks.isEmpty() -> "Belum ada tugas. Tambahkan deadline pertamamu lewat tombol +."
            filter == 1 -> "Belum ada tugas yang selesai. Satu per satu, ya."
            filter == 0 && overdue > 0 -> "Tidak ada tugas aktif, tetapi ada $overdue tugas terlambat di tab Semua."
            else -> "Tidak ada tugas pada filter ini."
        }
        b.tvCount.text = buildString {
            append("$active tugas aktif")
            if (overdue > 0) append(" · $overdue terlambat")
            append(" · ${allTasks.count { it.done }} selesai")
        }
    }

    private fun setDone(task: com.matfess.app.data.Task, done: Boolean) {
        repo.db.setTaskDone(task.id, done)
        if (done) {
            AlarmScheduler.cancelTask(requireContext(), task.id)
            toast("Tugas selesai. Alarm dimatikan.")
        } else {
            AlarmScheduler.scheduleTask(requireContext(), task.copy(done = false))
        }
        reload()
    }

    private fun confirmDelete(task: com.matfess.app.data.Task) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus tugas?")
            .setMessage("\"${task.title}\" akan dihapus beserta alarm pengingatnya.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repo.db.deleteTask(task.id)
                AlarmScheduler.cancelTask(requireContext(), task.id)
                reload()
                toast("Tugas dihapus.")
            }
            .show()
    }

    override fun onDestroyView() {
        _binding?.recycler?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
