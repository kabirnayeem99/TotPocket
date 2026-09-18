package io.github.kabirnayeem99.totpocket.home

import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter

private val TimeFormat = NSDateFormatter().apply { dateFormat = "h:mm" }
private val DateFormat = NSDateFormatter().apply { dateFormat = "EEE, d MMM" }

actual fun wallClockNow(): WallClock {
    val now = NSDate()
    return WallClock(time = TimeFormat.stringFromDate(now), date = DateFormat.stringFromDate(now))
}
