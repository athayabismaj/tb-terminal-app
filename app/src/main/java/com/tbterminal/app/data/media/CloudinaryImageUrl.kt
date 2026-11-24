package com.tbterminal.app.data.media

import java.net.URI

enum class ImageSize(val transformation: String) {
    THUMB_SMALL("c_fill,w_160,h_160,q_auto,f_webp"),
    THUMB_MEDIUM("c_fill,w_320,h_320,q_auto,f_webp"),
    FULL("c_limit,w_1080,q_auto,f_webp")
}

object CloudinaryImageUrl {
    private const val UPLOAD_MARKER = "/image/upload/"
    private val DIRECTIVE_REGEX = Regex("^(c|w|h|q|f|e|g|ar|dpr)_.*")

    fun transform(rawUrl: String?, size: ImageSize): String? {
        val trimmed = rawUrl?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val host = runCatching { URI(trimmed).host }.getOrNull()?.lowercase() ?: return trimmed
        if (host != "res.cloudinary.com" && !host.endsWith(".res.cloudinary.com")) {
            return trimmed
        }

        val markerIndex = trimmed.indexOf(UPLOAD_MARKER)
        if (markerIndex < 0) return trimmed

        val tail = trimmed.substring(markerIndex + UPLOAD_MARKER.length)
        if (tail.isEmpty()) return trimmed

        val firstSegment = tail.substringBefore('/')
        if (hasTransformationDirective(firstSegment)) {
            return trimmed
        }

        val prefix = trimmed.substring(0, markerIndex + UPLOAD_MARKER.length)
        return "$prefix${size.transformation}/$tail"
    }

    private fun hasTransformationDirective(segment: String): Boolean {
        if (segment.isEmpty()) return false
        if (segment.startsWith('v') && segment.length > 1 && segment.drop(1).all(Char::isDigit)) {
            return false
        }
        return segment.contains(',') || DIRECTIVE_REGEX.matches(segment)
    }
}
