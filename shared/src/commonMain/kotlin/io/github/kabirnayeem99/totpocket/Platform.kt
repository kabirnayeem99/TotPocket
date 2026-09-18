package io.github.kabirnayeem99.totpocket

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform