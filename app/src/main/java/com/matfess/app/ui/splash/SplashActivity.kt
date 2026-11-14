package com.matfess.app.ui.splash

import android.content.Intent
import android.os.Bundle
import android.view.animation.OvershootInterpolator
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.matfess.app.databinding.ActivitySplashBinding
import com.matfess.app.ui.auth.LoginActivity
import com.matfess.app.ui.common.BaseActivity
import com.matfess.app.ui.main.MainActivity
import com.matfess.app.ui.onboarding.OnboardingActivity
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : BaseActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.logo.scaleX = 0.6f
        binding.logo.scaleY = 0.6f
        binding.logo.alpha = 0f
        binding.brand.alpha = 0f
        binding.brand.translationY = 18f
        binding.tagline.alpha = 0f
        binding.tagline.translationY = 12f
        binding.featureStrip.alpha = 0f
        binding.featureStrip.translationY = 20f
        binding.bottomStatus.alpha = 0f

        binding.logo.animate()
            .alpha(1f).scaleX(1f).scaleY(1f)
            .setDuration(720)
            .setInterpolator(OvershootInterpolator(1.6f))
            .start()
        binding.brand.animate().alpha(1f).translationY(0f)
            .setStartDelay(260).setDuration(520).start()
        binding.tagline.animate().alpha(1f).translationY(0f)
            .setStartDelay(400).setDuration(520).start()
        binding.featureStrip.animate().alpha(1f).translationY(0f)
            .setStartDelay(560).setDuration(520).start()
        binding.bottomStatus.animate().alpha(1f).setStartDelay(700).setDuration(500).start()

        lifecycleScope.launch {
            delay(1_850)
            routeNext()
        }
    }

    private fun routeNext() {
        if (prefs.isLoggedIn && repo.db.getUser(prefs.userId) == null) {
            prefs.logout()
        }
        val next = when {
            !prefs.onboardingDone -> OnboardingActivity::class.java
            !prefs.isLoggedIn -> LoginActivity::class.java
            else -> MainActivity::class.java
        }
        startActivity(Intent(this, next))
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish()
    }
}
