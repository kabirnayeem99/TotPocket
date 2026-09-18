package io.github.kabirnayeem99.totpocket.home

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TimeFormat = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
private val DateFormat = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

actual fun wallClockNow(): WallClock {
    val now = LocalDateTime.now()
    return WallClock(time = now.format(TimeFormat), date = now.format(DateFormat))
}
