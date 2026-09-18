package io.github.kabirnayeem99.totpocket.device

/** Platform window/device behaviour that screens need to request, without touching an Activity. */
interface DeviceController {
    /** Keep the display awake — only while something the child is watching needs it (a call). */
    fun keepScreenOn(on: Boolean)

    /** Whether TotPocket is pinned (Android screen pinning / lock task). */
    val isPinned: Boolean

    /** Pins TotPocket so Home and Recents can't leave it. The system may ask the grown-up to confirm. */
    fun pin()

    fun unpin()

    /** Unpins and closes TotPocket. Only reachable from the parent settings. */
    fun exitApp()
}

/** For platforms (and previews) with nothing to control. */
object NoDeviceController : DeviceController {
    override fun keepScreenOn(on: Boolean) = Unit
    override val isPinned: Boolean = false
    override fun pin() = Unit
    override fun unpin() = Unit
    override fun exitApp() = Unit
}
