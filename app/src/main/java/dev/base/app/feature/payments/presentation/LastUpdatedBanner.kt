package dev.base.app.feature.payments.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import java.time.Clock
import java.time.Duration
import java.time.Instant

const val LAST_UPDATED_BANNER_TAG = "last_updated_banner"

/** "Sin conexión · Actualizado hace X min" (X redondeado; "hace menos de 1 min" si < 1). */
fun lastUpdatedText(staleSince: Instant, now: Instant): String {
    val minutes = Math.round(Duration.between(staleSince, now).toMillis().coerceAtLeast(0) / 60_000.0)
    val ago = if (minutes < 1) "hace menos de 1 min" else "hace $minutes min"
    return "Sin conexión · Actualizado $ago"
}

@Composable
fun LastUpdatedBanner(staleSince: Instant, modifier: Modifier = Modifier, clock: Clock = Clock.systemUTC()) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = modifier.fillMaxWidth().testTag(LAST_UPDATED_BANNER_TAG)
    ) {
        Text(
            text = lastUpdatedText(staleSince, clock.instant()),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
