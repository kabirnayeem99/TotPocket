package io.github.kabirnayeem99.totpocket.device

import android.app.Activity
import android.app.ActivityManager
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

    override val isPinned: Boolean
        get() {
            val activity = activityRef?.get() ?: return false
            val manager = activity.getSystemService(ActivityManager::class.java)
            return manager.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE
        }

    // Without device-owner provisioning this is Android "screen pinning": the system asks the
    // grown-up to confirm once, and unpinning needs Back + Recents (plus the PIN if they enabled it).
    override fun pin() {
        val activity = activityRef?.get() ?: return
        if (!isPinned) activity.runOnUiThread { activity.startLockTask() }
    }

    override fun unpin() {
        val activity = activityRef?.get() ?: return
        if (isPinned) activity.runOnUiThread { activity.stopLockTask() }
    }

    override fun exitApp() {
        val activity = activityRef?.get() ?: return
        activity.runOnUiThread {
            if (isPinned) activity.stopLockTask()
            activity.finishAndRemoveTask()
        }
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
