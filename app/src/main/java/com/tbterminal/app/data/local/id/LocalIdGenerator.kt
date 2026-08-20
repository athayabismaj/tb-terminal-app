package com.tbterminal.app.data.local.id

import java.util.UUID

object LocalIdGenerator {
    fun clientGeneratedId(prefix: String): String {
        val normalizedPrefix = prefix.trim().uppercase().ifEmpty { DEFAULT_PREFIX }
        return "$normalizedPrefix-${UUID.randomUUID()}"
    }

    private const val DEFAULT_PREFIX = "LOCAL"
}
