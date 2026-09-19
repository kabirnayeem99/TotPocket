package io.github.kabirnayeem99.totpocket.online

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Searches Wikimedia Commons photos through its public API — free, no key. Commons has no safe
 * search, so words that usually mean unsuitable pictures are excluded; a grown-up still looks at
 * every photo before adding it.
 */
class CommonsPhotoSearch : OnlinePhotoSearch {

    override suspend fun search(query: String): List<OnlinePhoto> = withContext(Dispatchers.IO) {
        val words = query.trim()
        if (words.isEmpty()) return@withContext emptyList()
        val params = mapOf(
            "action" to "query",
            "format" to "json",
            "generator" to "search",
            "gsrnamespace" to "6",
            "gsrlimit" to "30",
            "gsrsearch" to "$words filetype:bitmap " + EXCLUDED.joinToString(" ") { "-$it" },
            "prop" to "imageinfo",
            "iiprop" to "url|mime|extmetadata",
            "iiurlwidth" to "$IMAGE_WIDTH",
            "iiextmetadatafilter" to "LicenseShortName|Artist",
        )
        val url = API + "?" + params.entries.joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        val pages = Json.parseToJsonElement(get(url)).jsonObject["query"]?.jsonObject?.get("pages")?.jsonObject
            ?: return@withContext emptyList()
        pages.values.map { it.jsonObject }
            .sortedBy { it["index"]?.jsonPrimitive?.int ?: 0 }
            .mapNotNull { page ->
                val info = page["imageinfo"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@mapNotNull null
                if (info["mime"]?.jsonPrimitive?.content !in PHOTO_TYPES) return@mapNotNull null
                val image = info["thumburl"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val meta = info["extmetadata"]?.jsonObject
                val file = page["title"]?.jsonPrimitive?.content ?: return@mapNotNull null
                OnlinePhoto(
                    id = file,
                    title = file.removePrefix("File:").substringBeforeLast('.'),
                    // Commons serves standard thumbnail widths; the grid needs only a small one.
                    thumbUrl = image.replace("/${IMAGE_WIDTH}px-", "/${THUMB_WIDTH}px-"),
                    imageUrl = image,
                    author = meta.value("Artist").ifBlank { "Unknown" },
                    license = meta.value("LicenseShortName"),
                    pageUrl = info["descriptionurl"]?.jsonPrimitive?.content.orEmpty(),
                )
            }
    }

    private fun JsonObject?.value(key: String): String {
        val html = this?.get(key)?.jsonObject?.get("value")?.jsonPrimitive?.content.orEmpty()
        return html.replace(Regex("<[^>]+>"), "").replace(Regex("\\s+"), " ").trim()
    }

    companion object {
        private const val API = "https://commons.wikimedia.org/w/api.php"

        /** Wikimedia asks every client to name itself. */
        const val USER_AGENT = "TotPocket/1.0 (https://github.com/kabirnayeem99/TotPocket)"
        private const val IMAGE_WIDTH = 1280
        private const val THUMB_WIDTH = 330
        private val PHOTO_TYPES = setOf("image/jpeg", "image/png", "image/webp")
        private val EXCLUDED = listOf(
            "nude", "naked", "nudity", "erotic", "sex", "sexual", "porn", "topless", "bikini", "lingerie",
            "breast", "genital", "corpse", "dead", "blood", "gore", "wound", "weapon", "gun",
        )

        /** GETs [url] as text, failing on any non-200 answer. Call off the main thread. */
        fun get(url: String): String = open(url).use { it.readBytes().decodeToString() }

        /** Opens [url] for reading, failing on any non-200 answer. Call off the main thread. */
        fun open(url: String): java.io.InputStream {
            val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                setRequestProperty("User-Agent", USER_AGENT)
                connectTimeout = 15_000
                readTimeout = 20_000
            }
            val code = connection.responseCode
            if (code != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                throw IOException("HTTP $code for $url")
            }
            return connection.inputStream
        }
    }
}
