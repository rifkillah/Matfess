package com.matfess.app.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.R
import com.matfess.app.databinding.FragmentToolsBinding
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.util.applyTopInset

class ToolsFragment : BaseFragment() {

    private var _binding: FragmentToolsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentToolsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scroll.applyTopInset(6)
        binding.cardRandom.setOnClickListener { navigate(R.id.randomGroupFragment) }
        binding.cardEvents.setOnClickListener { navigate(R.id.eventsFragment) }
        binding.cardIpk.setOnClickListener { navigate(R.id.ipkFragment) }
        binding.cardGraduation.setOnClickListener {
            navigate(R.id.graduationFragment, Bundle().apply { putInt("tab", 0) })
        }
        binding.cardRetake.setOnClickListener {
            navigate(R.id.graduationFragment, Bundle().apply { putInt("tab", 1) })
        }
        binding.cardFocus.setOnClickListener { navigate(R.id.focusFragment) }
        binding.cardAbout.setOnClickListener { showAbout() }
    }

    private fun navigate(id: Int, args: Bundle? = null) {
        try {
            if (args == null) findNavController().navigate(id)
            else findNavController().navigate(id, args)
        } catch (_: IllegalArgumentException) {
            // Already on the destination.
        }
    }

    private fun showAbout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Matfess 1.0")
            .setMessage(
                "Asisten kuliah offline-first untuk mahasiswa.\n\n" +
                    "Dibangun dengan Kotlin, SQLite, RecyclerView, Navigation Component, " +
                    "custom glass views, dan Retrofit (kalender libur nasional + kutipan). " +
                    "Tema terang/gelap mengikuti pengaturan sistem atau pilihanmu.\n\n" +
                    "Semua data tersimpan lokal di perangkatmu."
            )
            .setPositiveButton("Tutup", null)
            .show()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
