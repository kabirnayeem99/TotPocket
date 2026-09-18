package io.github.kabirnayeem99.totpocket.device

import android.app.Activity
import android.view.WindowManager
import java.lang.ref.WeakReference

/**
 * [DeviceController] for the single TotPocket activity. The activity [attach]es itself on create
 * and [detach]es on destroy; requests made while no activity is attached are applied on attach.
 */
class AndroidDeviceController : DeviceController {

    private var activityRef: WeakReference<Activity>? = null
    private var screenOn = false

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
        applyScreenOn()
    }

    fun detach(activity: Activity) {
        if (activityRef?.get() === activity) activityRef = null
    }

    override fun keepScreenOn(on: Boolean) {
        screenOn = on
        applyScreenOn()
    }

    private fun applyScreenOn() {
        val activity = activityRef?.get() ?: return
        activity.runOnUiThread {
            if (screenOn) {
                activity.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
