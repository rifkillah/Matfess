package com.matfess.app.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringAnimation
import androidx.dynamicanimation.animation.SpringForce
import androidx.navigation.NavController
import androidx.navigation.NavOptions
import androidx.navigation.fragment.NavHostFragment
import com.matfess.app.R
import com.matfess.app.databinding.ActivityMainBinding
import com.matfess.app.ui.common.BaseActivity
import com.matfess.app.util.applyBottomMarginInset
import com.matfess.app.util.dp

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    private val topDestinations = listOf(
        R.id.homeFragment,
        R.id.academicFragment,
        R.id.focusFragment,
        R.id.toolsFragment,
        R.id.profileFragment
    )
    private var currentIndex = 0

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.tabBar.applyBottomMarginInset(10)

        val host = supportFragmentManager.findFragmentById(R.id.navHost) as NavHostFragment
        navController = host.navController
        currentIndex = topDestinations.indexOf(navController.currentDestination?.id ?: -1)
            .coerceAtLeast(0)

        // Custom glass tabs deliberately replace BottomNavigationView: the Material
        // widget double-applies window insets inside a floating bar on some API levels.
        tabItems().forEachIndexed { index, item ->
            item.setOnClickListener { selectTab(index) }
            bindTabPress(item)
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val index = topDestinations.indexOf(destination.id)
            if (index >= 0) {
                currentIndex = index
                showTabBar(true)
                updateTabs(index)
                binding.tabBar.post { moveLens(index, animate = true) }
            } else {
                showTabBar(false)
            }
        }

        binding.tabBar.post {
            updateTabs(currentIndex)
            moveLens(currentIndex, animate = false)
        }
        requestNotificationPermission()
        handleDestinationIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDestinationIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // After a theme/config recreation the NavController restores its
        // destination before our listener is attached; re-apply the highlight
        // so the selected tab always matches the visible screen.
        val index = topDestinations.indexOf(navController.currentDestination?.id ?: -1)
        if (index >= 0) {
            currentIndex = index
            updateTabs(index)
            binding.tabBar.post { moveLens(index, animate = false) }
        }
    }

    private fun tabItems() = listOf(
        binding.tabHome,
        binding.tabAcademic,
        binding.tabFocus,
        binding.tabTools,
        binding.tabProfile
    )

    private fun tabIcons() = listOf(
        binding.tabHomeIcon,
        binding.tabAcademicIcon,
        binding.tabFocusIcon,
        binding.tabToolsIcon,
        binding.tabProfileIcon
    )

    private fun tabLabels() = listOf(
        binding.tabHomeLabel,
        binding.tabAcademicLabel,
        binding.tabFocusLabel,
        binding.tabToolsLabel,
        binding.tabProfileLabel
    )

    private fun bindTabPress(item: android.view.View) {
        item.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN ->
                    view.animate().scaleX(0.94f).scaleY(0.94f).setDuration(110).start()
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                    view.animate().scaleX(1f).scaleY(1f).setDuration(220).start()
            }
            false
        }
    }

    private fun selectTab(index: Int) {
        if (navController.currentDestination?.id == topDestinations[index]) return
        navigateTopLevel(topDestinations[index])
    }

    private fun navigateTopLevel(destination: Int, args: Bundle? = null) {
        val startId = navController.graph.startDestinationId
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setRestoreState(true)
            .setPopUpTo(startId, false, true)
            .build()
        navController.navigate(destination, args, options)
    }

    private fun updateTabs(index: Int) {
        val active = ContextCompat.getColor(this, R.color.accent_primary)
        val inactive = ContextCompat.getColor(this, R.color.text_tertiary)
        tabIcons().forEachIndexed { i, icon ->
            val selected = i == index
            icon.imageTintList = ColorStateList.valueOf(if (selected) active else inactive)
            icon.animate()
                .scaleX(if (selected) 1f else 0.9f)
                .scaleY(if (selected) 1f else 0.9f)
                .setDuration(240)
                .start()
        }
        tabLabels().forEachIndexed { i, label ->
            val selected = i == index
            label.setTextColor(if (selected) active else inactive)
            label.alpha = if (selected) 1f else 0.82f
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun handleDestinationIntent(intent: Intent?) {
        when (intent?.getStringExtra("destination")) {
            "tasks" -> navigateTopLevel(R.id.academicFragment, Bundle().apply { putInt("page", 2) })
            "schedule" -> navigateTopLevel(R.id.academicFragment, Bundle().apply { putInt("page", 1) })
            "focus" -> navigateTopLevel(R.id.focusFragment)
        }
    }

    private fun showTabBar(visible: Boolean) {
        if (visible) {
            if (!binding.tabBar.isVisible) {
                binding.tabBar.isVisible = true
            }
            binding.tabBar.animate().translationY(0f).alpha(1f).setDuration(320).start()
        } else {
            binding.tabBar.animate()
                .translationY(binding.tabBar.height + dp(40f).toFloat())
                .alpha(0f)
                .setDuration(240)
                .withEndAction { binding.tabBar.isVisible = false }
                .start()
        }
    }

    private fun moveLens(index: Int, animate: Boolean) {
        val items = binding.tabItems
        val itemWidth = items.width / topDestinations.size.toFloat()
        if (itemWidth <= 0f) return
        val lensWidth = (itemWidth - dp(20f)).toInt().coerceAtLeast(dp(52f))
        val targetX = index * itemWidth + (itemWidth - lensWidth) / 2f
        val params = binding.tabLens.layoutParams
        if (params.width != lensWidth) {
            params.width = lensWidth
            binding.tabLens.layoutParams = params
        }
        if (animate) {
            SpringAnimation(binding.tabLens, DynamicAnimation.TRANSLATION_X, targetX).apply {
                spring = SpringForce(targetX).apply {
                    stiffness = SpringForce.STIFFNESS_LOW
                    dampingRatio = 0.78f
                }
                start()
            }
        } else {
            binding.tabLens.translationX = targetX
        }
    }
}
