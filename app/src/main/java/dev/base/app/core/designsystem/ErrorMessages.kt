package dev.base.app.core.designsystem

import androidx.annotation.StringRes
import dev.base.app.R
import dev.base.app.core.common.AppError

/** Traduce un [AppError] al texto que ve el usuario. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.Network -> R.string.error_network
    is AppError.Server -> R.string.error_server
    AppError.NotFound -> R.string.error_not_found
    AppError.Serialization, AppError.Unknown -> R.string.error_unknown
}
