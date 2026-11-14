package com.matfess.app.ui.tools

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.R
import com.matfess.app.databinding.FragmentIpkBinding
import com.matfess.app.databinding.ItemIpkCourseBinding
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.util.DateUtils
import com.matfess.app.util.applyVerticalInsets
import java.util.Locale

class IpkFragment : BaseFragment() {

    private var _binding: FragmentIpkBinding? = null
    private val binding get() = _binding!!

    private data class Row(val binding: ItemIpkCourseBinding, var grade: String)

    private val rows = mutableListOf<Row>()
    private val grades = listOf("A", "A-", "B+", "B", "B-", "C+", "C", "D", "E")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIpkBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scroll.applyVerticalInsets(6, 20)
        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnAddRow.setOnClickListener { addRow() }
        binding.btnCalculate.setOnClickListener { calculate(showToast = true) }

        val draft = prefs.ipkDraft
        if (draft.isBlank()) {
            addRow("Struktur Data", "3", "A")
            addRow("Basis Data Lanjut", "3", "B+")
            addRow("Pemrograman Mobile", "4", "A-")
            addRow("Jaringan Komputer", "3", "B")
        } else {
            draft.lineSequence().forEach { line ->
                val parts = line.split("~")
                if (parts.size >= 3) {
                    addRow(
                        name = parts[0],
                        sks = parts[1],
                        grade = parts[2].ifBlank { "A" }
                    )
                }
            }
        }
        calculate()
    }

    private fun addRow(name: String = "", sks: String = "", grade: String = "A") {
        val b = ItemIpkCourseBinding.inflate(layoutInflater, binding.courseContainer, false)
        val row = Row(b, grade.ifBlank { "A" })
        b.etName.setText(name)
        b.etSks.setText(sks)
        applyGrade(row)
        b.btnGrade.setOnClickListener { chooseGrade(row) }
        b.btnRemove.setOnClickListener {
            rows.remove(row)
            binding.courseContainer.removeView(b.root)
            calculate()
        }
        rows.add(row)
        binding.courseContainer.addView(b.root)
    }

    private fun chooseGrade(row: Row) {
        val checked = grades.indexOf(row.grade).coerceAtLeast(0)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Pilih nilai")
            .setSingleChoiceItems(grades.toTypedArray(), checked) { dialog, which ->
                row.grade = grades[which]
                applyGrade(row)
                dialog.dismiss()
                calculate()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun applyGrade(row: Row) {
        row.binding.btnGrade.text = row.grade
        val color = when (row.grade) {
            "A", "A-" -> ContextCompat.getColor(requireContext(), R.color.success)
            "B+", "B", "B-" -> ContextCompat.getColor(requireContext(), R.color.accent_primary)
            "C+", "C" -> ContextCompat.getColor(requireContext(), R.color.warning)
            else -> ContextCompat.getColor(requireContext(), R.color.danger)
        }
        row.binding.btnGrade.setTextColor(color)
        row.binding.btnGrade.backgroundTintList =
            android.content.res.ColorStateList.valueOf(
                com.matfess.app.util.Palette.withAlpha(color, 28)
            )
    }

    private fun calculate(showToast: Boolean = false) {
        val b = _binding ?: return
        var totalSks = 0
        var weighted = 0.0
        var valid = 0
        rows.forEach { row ->
            val sks = row.binding.etSks.text?.toString()?.toIntOrNull() ?: 0
            val name = row.binding.etName.text?.toString()?.trim().orEmpty()
            if (sks > 0 && name.isNotEmpty()) {
                totalSks += sks
                weighted += sks * DateUtils.sksPoint(row.grade)
                valid++
            }
        }
        if (totalSks == 0) {
            b.tvIpk.text = "—"
            b.tvPredikat.text = "Tambahkan mata kuliah dan SKS untuk mulai menghitung."
            b.tvTotalSks.text = "0 SKS dihitung"
            b.ringIpk.setProgress(0f)
            return
        }
        val ipk = weighted / totalSks
        b.tvIpk.text = String.format(Locale("id", "ID"), "%.2f", ipk)
        b.tvPredikat.text = when {
            ipk >= 3.5 -> "Cumlaude · luar biasa"
            ipk >= 3.0 -> "Sangat baik · pertahankan"
            ipk >= 2.5 -> "Baik · masih bisa naik"
            ipk >= 2.0 -> "Cukup · perlu strategi belajar"
            else -> "Perlu perbaikan · mulai dari mata kuliah tersulit"
        }
        b.tvTotalSks.text = "$totalSks SKS · $valid mata kuliah · rata-rata bobot ${
            String.format(Locale("id", "ID"), "%.2f", ipk)
        }"
        b.ringIpk.setProgress((ipk / 4.0).toFloat())
        saveDraft()
        if (showToast) toast("IPK simulasi: ${String.format(Locale("id", "ID"), "%.2f", ipk)}")
    }

    private fun saveDraft() {
        val data = rows.joinToString("\n") { row ->
            val name = row.binding.etName.text?.toString()?.trim().orEmpty()
                .replace("~", " ").replace("\n", " ")
            val sks = row.binding.etSks.text?.toString()?.trim().orEmpty()
            "$name~$sks~${row.grade}"
        }
        prefs.ipkDraft = data
    }

    override fun onPause() {
        if (_binding != null) saveDraft()
        super.onPause()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
