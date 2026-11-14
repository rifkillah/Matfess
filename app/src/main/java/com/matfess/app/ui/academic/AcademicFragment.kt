package com.matfess.app.ui.academic

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager2.widget.ViewPager2
import com.matfess.app.databinding.FragmentAcademicBinding
import com.matfess.app.ui.common.BaseFragment
import com.matfess.app.util.applyTopInset

class AcademicFragment : BaseFragment() {

    private var _binding: FragmentAcademicBinding? = null
    private val binding get() = _binding!!

    private val pages = listOf("Catatan", "Jadwal", "Tugas", "Berkas")
    private val subtitles = listOf(
        "Catatan kuliah yang tersimpan offline dan bisa dicari.",
        "Jadwal kelas lengkap dengan kontak dosen untuk WhatsApp.",
        "Deadline, prioritas, dan alarm pengingat yang bisa diatur.",
        "Modul dan sertifikat dalam satu brankas digital."
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAcademicBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.header.applyTopInset(8)

        val initial = (arguments?.getInt("page", 0) ?: 0).coerceIn(0, pages.lastIndex)
        binding.pager.adapter = AcademicPagerAdapter(this)
        binding.pager.offscreenPageLimit = 3
        binding.segmented.setItems(pages, initial)
        binding.pager.setCurrentItem(initial, false)
        updateSubtitle(initial)

        binding.segmented.setOnSegmentChanged { index ->
            binding.pager.setCurrentItem(index, true)
        }
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.segmented.select(position, notify = false)
                updateSubtitle(position)
            }
        })
    }

    private fun updateSubtitle(index: Int) {
        _binding?.tvSubtitle?.text = subtitles[index.coerceIn(0, subtitles.lastIndex)]
    }

    override fun onDestroyView() {
        _binding?.pager?.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
