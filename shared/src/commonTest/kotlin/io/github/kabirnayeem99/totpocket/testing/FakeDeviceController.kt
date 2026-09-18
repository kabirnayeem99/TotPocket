package io.github.kabirnayeem99.totpocket.testing

import io.github.kabirnayeem99.totpocket.device.DeviceController

class FakeDeviceController : DeviceController {
    var screenOn = false
        private set
    var exited = false
        private set

    override var isPinned: Boolean = false
        private set

    override fun keepScreenOn(on: Boolean) {
        screenOn = on
    }

    override fun pin() {
        isPinned = true
    }

    override fun unpin() {
        isPinned = false
    }

    var systemBarsAllowed = false
        private set

    override fun setSystemBarsAllowed(allowed: Boolean) {
        systemBarsAllowed = allowed
    }

    var homeApp = false
        private set

    override fun setHomeApp(enabled: Boolean, askToChoose: Boolean) {
        homeApp = enabled
    }

    override fun exitApp() {
        homeApp = false
        isPinned = false
        exited = true
    }
}
