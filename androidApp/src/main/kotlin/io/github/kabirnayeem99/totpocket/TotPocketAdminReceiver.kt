package io.github.kabirnayeem99.totpocket

import android.app.admin.DeviceAdminReceiver

/**
 * Lets a grown-up turn a spare phone into a TotPocket-only kiosk:
 * `adb shell dpm set-device-owner io.github.kabirnayeem99.totpocket/.TotPocketAdminReceiver`
 * (on a phone with no accounts added). Nothing here runs unless that's done.
 */
class TotPocketAdminReceiver : DeviceAdminReceiver()
