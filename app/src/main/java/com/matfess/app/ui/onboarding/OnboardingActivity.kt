package com.matfess.app.ui.onboarding

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.matfess.app.R
import com.matfess.app.databinding.ActivityOnboardingBinding
import com.matfess.app.ui.auth.LoginActivity
import com.matfess.app.ui.common.BaseActivity
import com.matfess.app.ui.common.LiquidButton
import com.matfess.app.util.applyVerticalInsets
import com.matfess.app.util.dp
import kotlin.math.abs

class OnboardingActivity : BaseActivity() {

    private lateinit var binding: ActivityOnboardingBinding
    private val dots = mutableListOf<android.view.View>()

    private val pages = listOf(
        OnboardingPage(
            eyebrow = "01 — Catatan",
            title = "Catat poin dosen tanpa kehilangan momen.",
            description = "Simpan catatan per mata kuliah, beri warna, dan pin hal yang paling penting. Semua tersimpan offline.",
            icon = R.drawable.ic_note,
            accent = 0,
            highlights = "Catatan pintar  ·  Tag warna  ·  Pencarian cepat"
        ),
        OnboardingPage(
            eyebrow = "02 — Ritme",
            title = "Deadline, jadwal, dan fokus dalam satu tarikan napas.",
            description = "Alarm pengingat tugas, jadwal kuliah dengan kontak dosen, serta timer fokus yang menjaga ritme belajarmu.",
            icon = R.drawable.ic_timer,
            accent = 2,
            highlights = "Alarm deadline  ·  WhatsApp dosen  ·  Mode tidur"
        ),
        OnboardingPage(
            eyebrow = "03 — Progres",
            title = "Lihat perjalananmu sampai benar-benar lulus.",
            description = "Pantau hari kuliah, persiapan UTS/UAS, kewajiban kelulusan, hingga brankas sertifikat non-matkul.",
            icon = R.drawable.ic_graduation,
            accent = 3,
            highlights = "Tracker semester  ·  Target IPK  ·  Brankas sertifikat"
        )
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding.content.applyVerticalInsets(6, 0)

        binding.pager.adapter = OnboardingPagerAdapter(this, pages)
        binding.pager.offscreenPageLimit = 1
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updatePage(position)
            }
        })
        binding.pager.setPageTransformer { page, position ->
            page.alpha = 0.45f + (1f - abs(position).coerceAtMost(1f)) * 0.55f
            page.scaleX = 0.92f + (1f - abs(position).coerceAtMost(1f)) * 0.08f
            page.scaleY = 0.92f + (1f - abs(position).coerceAtMost(1f)) * 0.08f
            page.translationX = -position * page.width * 0.12f
        }

        buildDots()
        updatePage(0)

        binding.btnSkip.setOnClickListener { finishOnboarding() }
        binding.btnNext.setOnClickListener {
            if (binding.pager.currentItem < pages.lastIndex) {
                binding.pager.currentItem = binding.pager.currentItem + 1
            } else {
                finishOnboarding()
            }
        }
    }

    private fun buildDots() {
        binding.indicator.removeAllViews()
        dots.clear()
        pages.indices.forEach { index ->
            val dot = android.view.View(this)
            val params = android.widget.LinearLayout.LayoutParams(dp(7f), dp(7f)).apply {
                marginEnd = dp(6f)
            }
            dot.layoutParams = params
            dot.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_soft)
            binding.indicator.addView(dot)
            dots.add(dot)
        }
    }

    private fun updatePage(position: Int) {
        binding.btnNext.text = if (position == pages.lastIndex) "Mulai sekarang" else "Lanjut"
        dots.forEachIndexed { index, dot ->
            val active = index == position
            dot.animate()
                .scaleX(if (active) 1f else 0.8f)
                .scaleY(if (active) 1f else 0.8f)
                .setDuration(220)
                .start()
            val params = dot.layoutParams as android.widget.LinearLayout.LayoutParams
            params.width = if (active) dp(24f) else dp(7f)
            dot.layoutParams = params
            dot.backgroundTintList = android.content.res.ColorStateList.valueOf(
                if (active) ContextCompat.getColor(this, R.color.accent_primary)
                else ContextCompat.getColor(this, R.color.glass_stroke_soft)
            )
        }
        binding.btnNext.setTintColor(
            if (position == pages.lastIndex) ContextCompat.getColor(this, R.color.accent_mint)
            else ContextCompat.getColor(this, R.color.accent_primary)
        )
    }

    private fun finishOnboarding() {
        prefs.onboardingDone = true
        startActivity(Intent(this, LoginActivity::class.java))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}

data class OnboardingPage(
    val eyebrow: String,
    val title: String,
    val description: String,
    val icon: Int,
    val accent: Int,
    val highlights: String
)
