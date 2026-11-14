package com.matfess.app.util

import android.view.View
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Editor sheets must open fully expanded. Material's default collapsed state
 * hides the save/delete row behind the peek height on smaller screens.
 */
fun BottomSheetDialog.expandFully() {
    window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    setOnShowListener {
        val sheet = findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return@setOnShowListener
        BottomSheetBehavior.from(sheet).apply {
            skipCollapsed = true
            state = BottomSheetBehavior.STATE_EXPANDED
        }
    }
}
