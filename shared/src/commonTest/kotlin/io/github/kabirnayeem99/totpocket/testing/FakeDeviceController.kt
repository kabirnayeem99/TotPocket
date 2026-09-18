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

    override fun exitApp() {
        isPinned = false
        exited = true
    }
}
