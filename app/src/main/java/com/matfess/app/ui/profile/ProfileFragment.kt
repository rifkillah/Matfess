package com.matfess.app.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.matfess.app.R
import com.matfess.app.databinding.FragmentProfileBinding
import com.matfess.app.databinding.SheetEditProfileBinding
import com.matfess.app.ui.auth.LoginActivity
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.util.DateUtils
import com.matfess.app.util.expandFully
import com.matfess.app.util.Palette
import com.matfess.app.util.applyTopInset
import com.matfess.app.util.entrance
import java.util.Locale

class ProfileFragment : BaseFragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.scroll.applyTopInset(4)

        binding.btnEdit.setOnClickListener { showEditProfile() }
        binding.cardNotes.setOnClickListener { openAcademic(0) }
        binding.cardTasks.setOnClickListener { openAcademic(2) }
        binding.cardFocus.setOnClickListener { navigate(R.id.focusFragment) }
        binding.rowGraduation.setOnClickListener { openGraduation(0) }
        binding.rowRetake.setOnClickListener { openGraduation(1) }
        binding.rowIpk.setOnClickListener { navigate(R.id.ipkFragment) }
        binding.rowAbout.setOnClickListener { showAbout() }
        binding.btnLogout.setOnClickListener { confirmLogout() }

        binding.segmentedTheme.setItems(listOf("Sistem", "Terang", "Gelap"), prefs.themeMode)
        binding.segmentedTheme.setOnSegmentChanged { index ->
            if (prefs.themeMode == index) return@setOnSegmentChanged
            // AppCompatDelegate applies the new mode and recreates active activities.
            prefs.themeMode = index
        }

        binding.profileCard.entrance(20)
        bindProfile()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) bindProfile()
    }

    private fun bindProfile() {
        val user = repo.db.getUser(prefs.userId)
        val name = user?.name ?: prefs.userName
        val nim = user?.nim ?: prefs.userNim
        val major = user?.major ?: prefs.userMajor
        val semester = user?.semester ?: prefs.userSemester
        val stats = repo.stats()

        binding.tvName.text = name
        binding.tvNim.text = "NIM ${nim.ifBlank { "—" }}"
        binding.tvMajor.text = major.ifBlank { "Program studi belum diisi" }
        binding.tvAvatar.text = initials(name)
        binding.avatar.backgroundTintList = android.content.res.ColorStateList.valueOf(
            Palette.initialColor(requireContext(), name)
        )

        val accent = ContextCompat.getColor(requireContext(), R.color.accent_primary)
        binding.tvSemester.text = "SEMESTER $semester"
        binding.tvSemester.setTextColor(accent)
        binding.tvSemester.backgroundTintList =
            android.content.res.ColorStateList.valueOf(Palette.withAlpha(accent, 32))

        val totalDays = DateUtils.daysBetween(prefs.semesterStart, prefs.semesterEnd) + 1
        val currentDay = DateUtils.semesterDay(prefs).coerceAtMost(totalDays.toInt())
        binding.tvSemesterDay.text = String.format(
            Locale.forLanguageTag("id-ID"),
            "Hari ke-%d dari %d hari perkuliahan · %s",
            currentDay,
            totalDays,
            DateUtils.formatMonthYear(prefs.semesterEnd)
        )

        binding.tvStatNotes.text = stats.notes.toString()
        binding.tvStatTasks.text = stats.pendingTasks.toString()
        binding.tvStatFocus.text = stats.focusMinutes.toString()

        val graduation = repo.db.getGraduationItems()
        val done = graduation.count { it.done }
        binding.tvGraduationSub.text = if (graduation.isEmpty()) {
            "Belum ada syarat tercatat"
        } else {
            "$done dari ${graduation.size} syarat selesai"
        }

        val revisions = repo.db.getRevisions()
        binding.tvRetakeSub.text = if (revisions.isEmpty()) {
            "Belum ada mata kuliah ulang"
        } else {
            "${revisions.size} mata kuliah tercatat"
        }

        binding.segmentedTheme.setItems(listOf("Sistem", "Terang", "Gelap"), prefs.themeMode)
    }

    private fun showEditProfile() {
        val sheetBinding = SheetEditProfileBinding.inflate(layoutInflater)
        val dialog = BottomSheetDialog(requireContext(), R.style.Widget_Matfess_Sheet)
        dialog.setContentView(sheetBinding.root)
        dialog.expandFully()

        val user = repo.db.getUser(prefs.userId)
        sheetBinding.tvNim.text = user?.nim ?: prefs.userNim.ifBlank { "—" }
        sheetBinding.etName.setText(user?.name ?: prefs.userName)
        sheetBinding.etMajor.setText(user?.major ?: prefs.userMajor)
        sheetBinding.etSemester.setText((user?.semester ?: prefs.userSemester).toString())

        sheetBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        sheetBinding.btnSave.setOnClickListener {
            sheetBinding.tvError.visibility = View.GONE
            val name = sheetBinding.etName.text?.toString()?.trim().orEmpty()
            val major = sheetBinding.etMajor.text?.toString()?.trim().orEmpty()
            val semester = sheetBinding.etSemester.text?.toString()?.trim()?.toIntOrNull()

            when {
                name.isEmpty() -> {
                    sheetBinding.etName.error = "Nama wajib diisi"
                    sheetBinding.etName.requestFocus()
                    return@setOnClickListener
                }
                major.isEmpty() -> {
                    sheetBinding.etMajor.error = "Program studi wajib diisi"
                    sheetBinding.etMajor.requestFocus()
                    return@setOnClickListener
                }
                semester == null || semester !in 1..14 -> {
                    sheetBinding.tvError.text = "Semester harus antara 1 sampai 14."
                    sheetBinding.tvError.visibility = View.VISIBLE
                    return@setOnClickListener
                }
            }
            val id = prefs.userId
            if (id <= 0) {
                sheetBinding.tvError.text = "Sesi tidak ditemukan. Silakan masuk ulang."
                sheetBinding.tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }
            repo.db.updateUser(id, name, major, semester!!)
            prefs.userName = name
            prefs.userMajor = major
            prefs.userSemester = semester
            dialog.dismiss()
            bindProfile()
            toast("Profil diperbarui.")
        }
        dialog.show()
    }

    private fun openAcademic(page: Int) {
        navigate(R.id.academicFragment, Bundle().apply { putInt("page", page) })
    }

    private fun openGraduation(tab: Int) {
        navigate(R.id.graduationFragment, Bundle().apply { putInt("tab", tab) })
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
                    "Semua data akademik tersimpan lokal di perangkatmu.\n\n" +
                    "Tema terang, gelap, atau ikuti sistem — pilih di halaman ini."
            )
            .setPositiveButton("Tutup", null)
            .show()
    }

    private fun confirmLogout() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Keluar dari akun?")
            .setMessage("Data kuliahmu tetap tersimpan di perangkat dan bisa dibuka lagi setelah masuk.")
            .setNegativeButton("Batal", null)
            .setPositiveButton("Keluar") { _, _ ->
                prefs.logout()
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
            .show()
    }

    private fun initials(name: String): String {
        val parts = name.trim().split(" ").filter { it.isNotBlank() }
        return when {
            parts.isEmpty() -> "M"
            parts.size == 1 -> parts[0].take(2).uppercase(Locale.forLanguageTag("id-ID"))
            else -> (parts[0].take(1) + parts[1].take(1)).uppercase(Locale.forLanguageTag("id-ID"))
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
