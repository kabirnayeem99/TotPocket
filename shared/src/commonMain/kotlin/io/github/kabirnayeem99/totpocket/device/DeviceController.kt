package io.github.kabirnayeem99.totpocket.device

/** Platform window/device behaviour that screens need to request, without touching an Activity. */
interface DeviceController {
    /** Keep the display awake — only while something the child is watching needs it (a call). */
    fun keepScreenOn(on: Boolean)
}

/** For platforms (and previews) with nothing to control. */
object NoDeviceController : DeviceController {
    override fun keepScreenOn(on: Boolean) = Unit
}
