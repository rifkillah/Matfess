package com.matfess.app.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.R
import com.matfess.app.data.GraduationItem
import com.matfess.app.data.Revision
import com.matfess.app.databinding.FragmentGraduationBinding
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.util.DateUtils
import com.matfess.app.util.applyVerticalInsets
import com.matfess.app.util.entrance
import java.util.Locale

/**
 * "Menuju kelulusan" hub: graduation requirements and retake courses live
 * in one screen, switched with the liquid segmented control.
 */
class GraduationFragment : BaseFragment() {

    private var _binding: FragmentGraduationBinding? = null
    private val binding get() = _binding!!

    private lateinit var graduationAdapter: GraduationAdapter
    private lateinit var revisionAdapter: RevisionAdapter

    private var tab = 0
    private var items: List<GraduationItem> = emptyList()
    private var revisions: List<Revision> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGraduationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.root.applyVerticalInsets(0, 8)
        tab = arguments?.getInt("tab", 0)?.coerceIn(0, 1) ?: 0

        graduationAdapter = GraduationAdapter(
            onToggle = { item, done ->
                repo.db.setGraduationDone(item.id, done)
                refresh()
                if (done) toast("Satu langkah lagi selesai. Mantap!")
            },
            onClick = { showGraduationEditor(it) { refresh() } },
            onLongClick = { confirmDeleteGraduation(it) }
        )
        revisionAdapter = RevisionAdapter(
            onClick = { showRevisionEditor(it) { refresh() } },
            onLongClick = { confirmDeleteRevision(it) }
        )

        binding.rvGraduation.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGraduation.adapter = graduationAdapter
        binding.rvGraduation.itemAnimator = null
        binding.rvRevisions.layoutManager = LinearLayoutManager(requireContext())
        binding.rvRevisions.adapter = revisionAdapter
        binding.rvRevisions.itemAnimator = null

        binding.btnBack.setOnClickListener { findNavController().navigateUp() }
        binding.btnAdd.setOnClickListener {
            if (tab == 0) showGraduationEditor(null) { refresh() }
            else showRevisionEditor(null) { refresh() }
        }

        binding.segmented.setItems(listOf("Syarat lulus", "Mata kuliah ulang"), tab)
        binding.segmented.setOnSegmentChanged { index ->
            tab = index
            applyTab()
        }

        binding.progressCard.entrance(40)
        binding.segmented.entrance(0)
        applyTab()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) refresh()
    }

    private fun applyTab() {
        val isGraduation = tab == 0
        binding.rvGraduation.isVisible = isGraduation
        binding.rvRevisions.isVisible = !isGraduation
        binding.btnAdd.contentDescription =
            if (isGraduation) "Tambah syarat kelulusan" else "Tambah mata kuliah ulang"
        refresh()
        val target = if (isGraduation) binding.rvGraduation else binding.rvRevisions
        target.alpha = 0f
        target.translationY = 12f
        target.animate().alpha(1f).translationY(0f).setDuration(320).start()
    }

    private fun refresh() {
        if (_binding == null) return
        items = repo.db.getGraduationItems()
        revisions = repo.db.getRevisions()
        graduationAdapter.submitList(items)
        revisionAdapter.submitList(revisions)

        val done = items.count { it.done }
        val total = items.size
        val progress = if (total == 0) 0f else done.toFloat() / total
        val next = items.filter { !it.done }.minByOrNull { it.targetDate }

        if (tab == 0) {
            binding.ringProgress.setProgress(progress)
            binding.ringProgress.setText("${(progress * 100).toInt()}%")
            binding.ringProgress.setSubText("SELESAI")
            binding.tvProgressTitle.text = if (total == 0) {
                "Belum ada syarat kelulusan"
            } else {
                "$done dari $total syarat selesai"
            }
            binding.tvProgressSub.text = when {
                total == 0 -> "Tambahkan target agar progresmu terukur."
                done == total -> "Semua syarat tuntas. Tinggal jaga momentum!"
                next != null -> "Berikutnya: ${next.title} · ${DateUtils.formatShort(next.targetDate)}"
                else -> "Langkah kecil tiap hari tetap berarti."
            }
            binding.tvStatus.text = when {
                total == 0 -> "Ketuk tombol + untuk menambahkan syarat kelulusan pertamamu."
                done == total -> "Selamat! Semua target kelulusan sudah kamu centang."
                else -> String.format(
                    Locale.forLanguageTag("id-ID"),
                    "Tersisa %d syarat. Target terdekat %s (%s).",
                    total - done,
                    next?.title.orEmpty(),
                    next?.let { DateUtils.formatFull(it.targetDate) }.orEmpty()
                )
            }
            binding.tvEmpty.text = "Belum ada syarat kelulusan.\nTambahkan skripsi, administrasi, sertifikasi, atau pengalaman."
            binding.tvEmpty.isVisible = total == 0
            binding.rvGraduation.isVisible = total > 0
        } else {
            val scheduled = revisions.count { it.targetSemester.isNotBlank() }
            binding.ringProgress.setProgress(
                if (revisions.isEmpty()) 0f else scheduled.toFloat() / revisions.size
            )
            binding.ringProgress.setText(revisions.size.toString())
            binding.ringProgress.setSubText("MK ULANG")
            binding.tvProgressTitle.text = if (revisions.isEmpty()) {
                "Belum ada mata kuliah ulang"
            } else {
                "${revisions.size} mata kuliah perlu diulang"
            }
            binding.tvProgressSub.text = when {
                revisions.isEmpty() -> "Catat mata kuliah dengan nilai C atau D."
                scheduled == revisions.size -> "Semua sudah punya target semester."
                else -> "$scheduled dari ${revisions.size} sudah dijadwalkan ulang."
            }
            binding.tvStatus.text = if (revisions.isEmpty()) {
                "Ketuk + untuk mencatat mata kuliah yang ingin diperbaiki nilainya."
            } else {
                "Rencanakan ulang dari mata kuliah prasyarat agar tidak menumpuk di semester akhir."
            }
            binding.tvEmpty.text = "Belum ada daftar ulang.\nTambahkan mata kuliah yang ingin kamu perbaiki."
            binding.tvEmpty.isVisible = revisions.isEmpty()
            binding.rvRevisions.isVisible = revisions.isNotEmpty()
        }
    }

    private fun confirmDeleteGraduation(item: GraduationItem) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus syarat?")
            .setMessage("\"${item.title}\" akan dihapus dari daftar kelulusan.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repo.db.deleteGraduation(item.id)
                refresh()
                toast("Syarat dihapus.")
            }
            .show()
    }

    private fun confirmDeleteRevision(revision: Revision) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Hapus mata kuliah ulang?")
            .setMessage("\"${revision.course}\" akan dihapus dari daftar.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Hapus") { _, _ ->
                repo.db.deleteRevision(revision.id)
                refresh()
                toast("Mata kuliah ulang dihapus.")
            }
            .show()
    }

    override fun onDestroyView() {
        _binding?.rvGraduation?.adapter = null
        _binding?.rvRevisions?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
