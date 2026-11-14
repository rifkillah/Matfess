package com.matfess.app.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.databinding.FragmentEventsBinding
import com.matfess.app.network.RemoteRepository
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.util.applyVerticalInsets
import kotlinx.coroutines.launch
import java.time.LocalDate

class EventsFragment : BaseFragment() {

    private var _binding: FragmentEventsBinding? = null
    private val binding get() = _binding!!
    private lateinit var eventAdapter: EventAdapter
    private val holidayAdapter = HolidayAdapter()
    private var tab = 0
    private var events: List<com.matfess.app.data.CampusEvent> = emptyList()
    private var holidaysLoaded = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEventsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyVerticalInsets(6, 10)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        eventAdapter = EventAdapter(
            onClick = { event -> showEventEditor(event) { reloadEvents() } },
            onLongClick = { event -> confirmDelete(event) }
        )
        binding.rvEvents.layoutManager = LinearLayoutManager(requireContext())
        binding.rvEvents.adapter = eventAdapter
        binding.rvHolidays.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHolidays.adapter = holidayAdapter

        binding.segmented.setItems(listOf("Agenda kampus", "Libur nasional"), 0)
        binding.segmented.setOnSegmentChanged {
            tab = it
            renderTab()
        }
        binding.btnAdd.setOnClickListener {
            showEventEditor(null) { reloadEvents() }
        }
        renderTab()
        reloadEvents()
        loadHolidays()
    }

    private fun renderTab() {
        val b = _binding ?: return
        b.rvEvents.isVisible = tab == 0
        b.rvHolidays.isVisible = tab == 1
        b.btnAdd.isVisible = tab == 0
        if (tab == 0) {
            renderEventsState()
        } else {
            renderHolidaysState()
        }
    }

    private fun reloadEvents() {
        if (_binding == null) return
        events = repo.db.getEvents().sortedBy { it.date }
        eventAdapter.submitList(events)
        renderEventsState()
    }

    private fun renderEventsState() {
        val b = _binding ?: return
        if (tab != 0) return
        b.tvStatus.text = "${events.size} agenda kampus tersimpan"
        b.tvEmpty.isVisible = events.isEmpty()
        b.tvEmpty.text = "Belum ada agenda. Tambahkan KRS, UKT, seminar, atau kegiatan kampus lainnya."
    }

    private fun loadHolidays() {
        if (holidaysLoaded) return
        val b = _binding ?: return
        b.tvStatus.text = "Memuat kalender libur nasional…"
        viewLifecycleOwner.lifecycleScope.launch {
            val year = LocalDate.now().year
            val result = RemoteRepository.holidays(year)
            holidaysLoaded = true
            holidayAdapter.submitList(result.items)
            if (_binding != null && tab == 1) {
                renderHolidaysState()
            }
        }
    }

    private fun renderHolidaysState() {
        val b = _binding ?: return
        b.tvStatus.text = if (holidaysLoaded) {
            "Libur nasional Indonesia · ${LocalDate.now().year} · data online/luring otomatis"
        } else {
            "Memuat kalender libur nasional…"
        }
        b.tvEmpty.isVisible = holidaysLoaded && holidayAdapter.itemCount == 0
        b.tvEmpty.text = "Tidak ada data libur nasional untuk tahun ini."
    }

    private fun confirmDelete(event: com.matfess.app.data.CampusEvent) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus agenda?")
            .setMessage("\"${event.title}\" akan dihapus dari kalender kampus.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repo.db.deleteEvent(event.id)
                reloadEvents()
                toast("Agenda dihapus.")
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) reloadEvents()
    }

    override fun onDestroyView() {
        _binding?.rvEvents?.adapter = null
        _binding?.rvHolidays?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
