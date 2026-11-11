package dev.base.app.feature.home.domain

import dev.base.app.core.common.AppResult

/** Feature de referencia: copia esta estructura (domain / data / presentation). */
data class Tip(val id: Int, val title: String)

interface TipRepository {
    suspend fun getTips(): AppResult<List<Tip>>
}
