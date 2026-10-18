package com.mvvmmovie.view

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

/**
 * targetSdk 36 draws edge to edge from Android 15, so the content starts under the status bar
 * and the action bar. This pads [root] clear of both.
 *
 * The top inset can be used as is. Edge to edge, AppCompat's action bar layout adds the bar's own
 * height to the inset it hands the content (and, since AppCompat 1.8, stretches the bar's
 * background up behind the status bar, so the theme's light status bar icons stay readable);
 * below Android 15 it makes room with a margin instead and hands down a top inset of 0, so
 * nothing is counted twice.
 */
fun fitContentToSystemBars(root: View) {
    ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
        val bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
        view.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
        windowInsets
    }
}
