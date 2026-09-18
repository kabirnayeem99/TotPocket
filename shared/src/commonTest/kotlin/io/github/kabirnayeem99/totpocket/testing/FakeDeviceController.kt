package io.github.kabirnayeem99.totpocket.testing

import io.github.kabirnayeem99.totpocket.device.DeviceController

class FakeDeviceController : DeviceController {
    var screenOn = false
        private set

    override fun keepScreenOn(on: Boolean) {
        screenOn = on
    }
}
