package ru.whiteleaf.notes.common.utils

import android.view.WindowManager
import androidx.fragment.app.FragmentActivity

fun toggleSecurePreview(activity: FragmentActivity, isSecure: Boolean) {
    println("DEBUG: toggleSecurePreview isSecure: $isSecure")
    if (isSecure) {
        activity.window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    } else {
        activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }
}