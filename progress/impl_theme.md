# impl_theme

## Hecho
- `app/src/main/java/dev/base/app/core/designsystem/Theme.kt`: `LightColors`/`DarkColors` reescritos con los tokens `--l-*`/`--d-*` de `docs/design/mockup.html` (background/surface, surfaceContainer, surfaceVariant + surfaceContainerHighest, onSurface/onBackground, onSurfaceVariant, primary/onPrimary, primaryContainer/onPrimaryContainer, errorContainer/onErrorContainer, tertiaryContainer/onTertiaryContainer). `isSystemInDarkTheme()` se mantiene; sin dynamic color.
- `ExtendedColors` (nombres sin cambios): success/successContainer = `#1F6F5C` con on* `#FFFFFF` (claro); `#7FD1B6` con on* `#00382B` (oscuro). Pagado = relleno sólido de marca.
- Se conservan `secondary` y `error` existentes (el mockup no los define).
- `./gradlew --console=plain --quiet assembleDebug` pasa.

## Barra de estado
- No se tocó `themes.xml` ni `MainActivity`. `enableEdgeToEdge()` sin argumentos usa `SystemBarStyle.auto`, que elige íconos oscuros/claros según el modo del sistema; como el tema también sigue al sistema, los íconos quedan legibles en ambos (oscuros sobre `#E7EFEB`, claros sobre `#13201C`).
- Limitación: si alguien llama `AppTheme(darkTheme = X)` forzando un modo distinto al del sistema, los íconos no lo seguirían; harían falta cambios en MainActivity (fuera de alcance).

## Notas
- Ningún archivo usa hoy `AppColors`; `PaymentsScreen.kt:164` usa `secondary` para "Pagado" (dice que no hay token success). No lo toqué (es de `feature/payments/**`); si se quiere el Pagado de marca, ese chip debe pasar a `AppColors.current.success`/`onSuccess`.
- Tokens del mockup no mapeados (warn/banner, dialog, snackbar, scrim) quedan con los valores por defecto de M3.
