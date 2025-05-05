package com.alex.testapp.util

import android.app.Activity
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

object UIHelper {

    fun applyTransparentStatusBar(activity: Activity, rootView: View) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)

        ViewCompat.setOnApplyWindowInsetsListener(rootView) { v, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            val paddingStart = v.paddingStart
            val paddingTop = v.paddingTop
            val paddingEnd = v.paddingEnd
            val paddingBottom = v.paddingBottom

            v.setPaddingRelative(
                paddingStart,
                paddingTop + statusBarHeight,
                paddingEnd,
                paddingBottom
            )
            insets
        }

        @Suppress("DEPRECATION")
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            activity.window.insetsController?.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            activity.window.statusBarColor = Color.TRANSPARENT
        }
    }
}
