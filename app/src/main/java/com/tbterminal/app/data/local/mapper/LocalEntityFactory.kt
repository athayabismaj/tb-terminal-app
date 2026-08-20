package com.tbterminal.app.data.local.mapper

import java.util.UUID

object LocalEntityFactory {
    fun newClientGeneratedId(prefix: String): String {
        return "${prefix}-${UUID.randomUUID()}"
    }
}
