package io.github.kabirnayeem99.totpocket.device

import android.app.Activity
import android.app.ActivityManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import android.view.WindowManager
import java.lang.ref.WeakReference

/**
 * [DeviceController] for the single TotPocket activity. The activity [attach]es itself on create
 * and [detach]es on destroy; requests made while no activity is attached are applied on attach.
 *
 * Two levels of lock:
 * - **Screen pinning** (any phone): the system asks the grown-up to confirm once; Home, Recents
 *   and the notification shade are blocked while pinned.
 * - **Kiosk** (TotPocket set as device owner with `dpm set-device-owner`): locks with no prompt,
 *   and the status bar and notifications are switched off by the system unless allowed.
 */
class AndroidDeviceController(
    private val admin: ComponentName,
    /** The activity-alias carrying the HOME intent filter; disabled unless the grown-up opts in. */
    private val homeAlias: ComponentName,
) : DeviceController {

    private var activityRef: WeakReference<Activity>? = null
    private var screenOn = false
    private var systemBarsAllowed = false

    fun attach(activity: Activity) {
        activityRef = WeakReference(activity)
        applyScreenOn()
        applyKioskPolicy()
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

    override fun pin() {
        val activity = activityRef?.get() ?: return
        applyKioskPolicy()
        if (!isPinned) activity.runOnUiThread { activity.startLockTask() }
    }

    override fun unpin() {
        val activity = activityRef?.get() ?: return
        if (isPinned) activity.runOnUiThread { activity.stopLockTask() }
    }

    override fun setSystemBarsAllowed(allowed: Boolean) {
        systemBarsAllowed = allowed
        applyKioskPolicy()
    }

    override fun setHomeApp(enabled: Boolean, askToChoose: Boolean) {
        val activity = activityRef?.get() ?: return
        activity.packageManager.setComponentEnabledSetting(
            homeAlias,
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP,
        )
        if (enabled && askToChoose) {
            activity.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    override fun exitApp() {
        val activity = activityRef?.get() ?: return
        // Hand Home back to the phone's own launcher, or it would just reopen TotPocket.
        setHomeApp(enabled = false, askToChoose = false)
        activity.runOnUiThread {
            if (isPinned) activity.stopLockTask()
            activity.finishAndRemoveTask()
        }
    }

    /** Only has an effect when TotPocket is the device owner. */
    private fun applyKioskPolicy() {
        val activity = activityRef?.get() ?: return
        val policy = activity.getSystemService(DevicePolicyManager::class.java)
        if (!policy.isDeviceOwnerApp(activity.packageName)) return
        policy.setLockTaskPackages(admin, arrayOf(activity.packageName))
        policy.setLockTaskFeatures(
            admin,
            if (systemBarsAllowed) {
                DevicePolicyManager.LOCK_TASK_FEATURE_SYSTEM_INFO or DevicePolicyManager.LOCK_TASK_FEATURE_NOTIFICATIONS
            } else {
                DevicePolicyManager.LOCK_TASK_FEATURE_NONE
            },
        )
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
