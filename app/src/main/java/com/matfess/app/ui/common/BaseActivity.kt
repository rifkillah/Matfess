package com.matfess.app.ui.common

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.matfess.app.data.MatfessRepository
import com.matfess.app.data.Prefs

/**
 * Activity base class that applies edge-to-edge drawing, keeps status bar icons
 * readable, and logs every lifecycle callback for the class demo.
 */
open class BaseActivity : AppCompatActivity() {

    protected lateinit var repo: MatfessRepository
        private set
    protected lateinit var prefs: Prefs
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        repo = MatfessRepository(this)
        prefs = Prefs(this)
        syncSystemBars()
    }

    protected fun syncSystemBars() {
        val night = (resources.configuration.uiMode and
                android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = !night
            isAppearanceLightNavigationBars = !night
        }
    }

    protected fun lifecycleLog(event: String) {
        Log.d("MatfessLifecycle", "${javaClass.simpleName} → $event")
    }

    override fun onStart() {
        super.onStart()
        lifecycleLog("onStart()")
    }

    override fun onResume() {
        super.onResume()
        lifecycleLog("onResume()")
    }

    override fun onPause() {
        lifecycleLog("onPause()")
        super.onPause()
    }

    override fun onStop() {
        lifecycleLog("onStop()")
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleLog("onDestroy()")
        super.onDestroy()
    }
}
