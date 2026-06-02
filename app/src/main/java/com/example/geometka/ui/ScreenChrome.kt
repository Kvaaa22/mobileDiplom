package com.example.geometka.ui

import android.app.Activity
import android.content.Intent
import android.view.View

object ScreenChrome {

    fun apply(activity: Activity) {
        activity.window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }

    fun navigateWithoutJump(
        activity: Activity,
        intent: Intent,
        finishCurrent: Boolean = true
    ) {
        activity.startActivity(intent)
        activity.overridePendingTransition(0, 0)

        if (finishCurrent) {
            activity.finish()
            activity.overridePendingTransition(0, 0)
        }
    }
}
