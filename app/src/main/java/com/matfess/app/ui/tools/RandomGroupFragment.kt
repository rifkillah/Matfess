package com.matfess.app.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.matfess.app.databinding.FragmentRandomGroupBinding
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.util.applyVerticalInsets

class RandomGroupFragment : BaseFragment() {

    private var _binding: FragmentRandomGroupBinding? = null
    private val binding get() = _binding!!
    private val adapter = GroupAdapter()

    private val sampleNames = listOf(
        "Rani", "Budi", "Anisa", "Fajar", "Dimas", "Sinta", "Reza", "Nadia", "Yoga", "Tiara"
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRandomGroupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scroll.applyVerticalInsets(6, 20)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }

        binding.rvGroups.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGroups.adapter = adapter
        binding.rvGroups.itemAnimator = null

        binding.etNames.setText(sampleNames.joinToString("\n"))
        binding.segGroups.setItems(listOf("2", "3", "4", "5", "6"), 1)
        binding.tvResultSummary.isVisible = false

        binding.btnShuffle.setOnClickListener { shuffle() }
    }

    private fun shuffle() {
        val names = binding.etNames.text?.toString().orEmpty()
            .split("\n", ",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (names.size < 2) {
            toast("Masukkan minimal dua nama untuk diacak.")
            return
        }
        val requested = binding.segGroups.selectedIndex() + 2
        val groupCount = requested.coerceAtMost(names.size)
        if (groupCount < requested) {
            toast("Nama lebih sedikit dari jumlah kelompok, kelompok disesuaikan.")
        }
        val shuffled = names.shuffled()
        val groups = MutableList(groupCount) { mutableListOf<String>() }
        shuffled.forEachIndexed { index, name ->
            groups[index % groupCount].add(name)
        }
        val results = groups.mapIndexed { index, members -> GroupResult(index, members) }
        binding.tvEmpty.isVisible = false
        binding.tvResultSummary.isVisible = true
        binding.tvResultSummary.text =
            "${names.size} mahasiswa · $groupCount kelompok · " +
                "${results.minOf { it.members.size }}–${results.maxOf { it.members.size }} orang per kelompok"
        adapter.submitList(results) {
            binding.rvGroups.scheduleLayoutAnimation()
        }
    }

    override fun onDestroyView() {
        _binding?.rvGroups?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
