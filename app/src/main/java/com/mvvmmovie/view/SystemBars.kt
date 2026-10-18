package com.mvvmmovie.view

import android.content.res.Configuration
import android.os.Build
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * targetSdk 36 draws edge to edge from Android 15: the content starts under the status bar and
 * the action bar, and android:statusBarColor is ignored. This pads [root] clear of both bars and
 * picks status bar icons that stay readable on the window background now showing behind them.
 *
 * The top inset can be used as is. Edge to edge, AppCompat's action bar layout adds the bar's own
 * height to the inset it hands the content; below Android 15 it makes room with a margin instead
 * and hands down a top inset of 0, so nothing is counted twice.
 */
fun AppCompatActivity.fitContentToSystemBars(root: View) {
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
        val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
        windowInsets
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
        val nightMode = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES
        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = !nightMode
    }
}
