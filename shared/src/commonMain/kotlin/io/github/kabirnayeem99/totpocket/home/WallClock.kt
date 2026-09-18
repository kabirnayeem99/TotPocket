package io.github.kabirnayeem99.totpocket.home

import androidx.compose.runtime.Immutable

/** What the home-screen clock widget shows, already formatted for the device's locale. */
@Immutable
data class WallClock(val time: String, val date: String)

expect fun wallClockNow(): WallClock
