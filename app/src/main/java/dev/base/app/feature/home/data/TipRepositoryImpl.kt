package dev.base.app.feature.home.data

import dev.base.app.core.common.AppError
import dev.base.app.core.common.AppResult
import dev.base.app.feature.home.domain.Tip
import dev.base.app.feature.home.domain.TipRepository
import javax.inject.Inject

/**
 * Lee los tips de Room y, la primera vez, guarda los de ejemplo.
 * En una feature real, aquí también estaría la fuente remota (Api + DTOs + mappers):
 * emitir caché, refrescar de red con safeApiCall y guardar.
 */
class TipRepositoryImpl @Inject constructor(private val dao: TipDao) : TipRepository {
    override suspend fun getTips(): AppResult<List<Tip>> = try {
        if (dao.getAll().isEmpty()) dao.insertAll(DEFAULT_TIPS.map { it.toEntity() })
        AppResult.Success(dao.getAll().map { it.toDomain() })
    } catch (e: IllegalStateException) {
        AppResult.Failure(AppError.Unknown)
    }

    companion object {
        val DEFAULT_TIPS = listOf(
            Tip(1, "Empieza por la spec"),
            Tip(2, "Tests en rojo antes del código"),
            Tip(3, "verify.sh define terminado")
        )
    }
}
