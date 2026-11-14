package com.tenantmanager.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View

object HapticHelper {
    fun performConfirm(view: View?, enabled: Boolean = true) {
        if (!enabled || view == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            }
        } catch (_: Throwable) {
            // Ignore in tests or unsupported environments
        }
    }

    fun performClick(view: View?, enabled: Boolean = true) {
        if (!enabled || view == null) return
        try {
            view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Throwable) {
            // Ignore in tests or unsupported environments
        }
    }
}
