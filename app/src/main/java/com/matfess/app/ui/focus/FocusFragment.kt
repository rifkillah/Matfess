package com.matfess.app.ui.focus

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.databinding.FragmentFocusBinding
import com.matfess.app.service.DndHelper
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.util.applyTopInset

class FocusFragment : BaseFragment() {

    private var _binding: FragmentFocusBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFocusBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scroll.applyTopInset(6)
        binding.btnStartSession.setOnClickListener { openTimer() }
        binding.ring.setOnClickListener { openTimer() }
        binding.btnEditFocus.setOnClickListener { chooseDuration(isFocus = true) }
        binding.btnEditBreak.setOnClickListener { chooseDuration(isFocus = false) }
        binding.switchDnd.setOnCheckedChange { enabled ->
            prefs.dndWhileFocus = enabled
            if (enabled && !DndHelper.hasAccess(requireContext())) {
                toast("Izinkan akses Jangan Ganggu agar bisa aktif otomatis.")
                DndHelper.openAccessSettings(requireContext())
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) refresh()
    }

    private fun refresh() {
        val b = _binding ?: return
        b.ring.setProgress(1f, animate = false)
        b.ring.setText("${prefs.focusDuration}:00")
        b.ring.setSubText("FOKUS")
        b.tvSessionInfo.text =
            "${prefs.focusDuration} menit fokus · ${prefs.breakDuration} menit istirahat"
        b.tvFocusDuration.text = "${prefs.focusDuration} menit per sesi"
        b.tvBreakDuration.text = "${prefs.breakDuration} menit per jeda"
        b.tvTotalMinutes.text = prefs.focusMinutesTotal.toString()
        b.tvTotalSessions.text = prefs.focusSessionsTotal.toString()
        b.switchDnd.setChecked(prefs.dndWhileFocus, notify = false, animate = false)
    }

    private fun openTimer() {
        startActivity(Intent(requireContext(), StudyTimerActivity::class.java))
    }

    private fun chooseDuration(isFocus: Boolean) {
        val values = if (isFocus) {
            intArrayOf(15, 20, 25, 30, 45, 50, 60, 90)
        } else {
            intArrayOf(3, 5, 10, 15, 20, 30)
        }
        val current = if (isFocus) prefs.focusDuration else prefs.breakDuration
        val labels = values.map { "$it menit" }.toTypedArray()
        val checked = values.indexOf(current).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (isFocus) "Durasi sesi fokus" else "Durasi istirahat")
            .setSingleChoiceItems(labels, checked) { dialog, which ->
                if (isFocus) prefs.focusDuration = values[which]
                else prefs.breakDuration = values[which]
                dialog.dismiss()
                refresh()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
