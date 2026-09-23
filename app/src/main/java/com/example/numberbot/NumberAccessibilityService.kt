package com.example.numberbot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class NumberAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile var instance: NumberAccessibilityService? = null
        @Volatile var enabled: Boolean = false
        @Volatile var nextNumber: Int = 1

        fun tap(x: Float, y: Float, duration: Long = 1L): Boolean {
            val service = instance ?: return false
            if (!enabled) return false

            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0L, duration)
            return service.dispatchGesture(
                GestureDescription.Builder().addStroke(stroke).build(),
                null,
                null
            )
        }
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() {
        enabled = false
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }
}
