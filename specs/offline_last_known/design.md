# offline_last_known: diseño

- `CachingPaymentsRepository` decora `PaymentsRepositoryImpl` (bind en `PaymentsModule`): guarda `Loan` + `Instant` del último éxito; en fallo con caché devuelve el dato guardado y expone la antigüedad.
- `PaymentsUiState.Content` gana `staleSince: Instant? = null` (default para no romper los tests congelados de `payments_list`).
- `LastUpdatedBanner` sobre la lista; "Pagar" desactivado con datos viejos.

## Alternativa descartada
- Room: más robusto para listas grandes, pero para un solo `Loan` basta un JSON en SharedPreferences y no suma migraciones.
