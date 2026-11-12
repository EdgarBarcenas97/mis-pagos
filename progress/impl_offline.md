# impl offline_last_known

## Hecho
- `feature/payments/data/CachingPaymentsRepository.kt`: decorador `@Singleton` de `PaymentsRepository` (recibe `PaymentsRepositoryImpl`). Guarda el último `Loan` bueno + `Instant` en memoria y en `SharedPreferences` (`payments_cache`/`last_loan`) como JSON con `Network.json` (DTO propio `CachedLoan`/`CachedPayment`: amount como texto, dueDate `LocalDate` ISO, status crudo). Si el delegado falla y hay caché devuelve `Success(cache)` y marca `isStale=true`; sin caché propaga el `Failure`. Interfaz nueva `PaymentsCacheInfo { lastSuccessAt: StateFlow<Instant?>; isStale: StateFlow<Boolean> }`. Constructor interno `(delegate, Storage, Clock)` para tests; el `@Inject` usa `Clock.systemUTC()`.
- `PaymentsModule`: bind de `PaymentsRepository` -> `CachingPaymentsRepository`.
- `PaymentsViewModel`: `Content(loanId, payments, staleSince: Instant? = null)`. El constructor no cambia: si el repositorio implementa `PaymentsCacheInfo` y `isStale`, `staleSince = lastSuccessAt`. Error solo si no hay caché.
- `presentation/LastUpdatedBanner.kt`: `lastUpdatedText(staleSince, now)` ("Sin conexión · Actualizado hace X min", X redondeado, "hace menos de 1 min" si < 1) y `LastUpdatedBanner` con testTag `last_updated_banner`.
- `PaymentsScreen.kt` (después del commit `2db87d3 feat(pay_payment)`): solo la rama `Content` envuelta en `Column` con el banner encima de la lista cuando `staleSince != null`.

## Verificación
`./gradlew --console=plain --quiet testDebugUnitTest assembleDebug` en verde (incluye payments_list y pay_payment).

## Desviaciones / pendientes
- **Pagar desactivado: NO hecho.** En el `PaymentsScreen.kt` commiteado por pay_payment no existe todavía el botón Pagar (queda `TODO(pay_payment T4)`), así que no hay nada que desactivar. Cuando exista hay que pasar `enabled = state.staleSince == null` al botón (dejé un comentario en la rama Content).
- El banner calcula "hace X min" al componer; no se refresca solo con el paso del tiempo (no hay ticker).
- `Clock` se crea en el constructor `@Inject` en vez de proveerse por Hilt (no había `Clock` en el grafo; evitar tocar más módulos).

## Tests que faltan (gate: no se escriben en esta fase)
- `CachingPaymentsRepositoryTest`: éxito guarda y fija `lastSuccessAt` con el Clock; fallo con caché -> Success + `isStale`; fallo sin caché -> Failure; persistencia: una instancia nueva con el mismo Storage recupera el dato; JSON corrupto se ignora; ida y vuelta de todos los `PaymentStatus` y BigDecimal.
- `PaymentsViewModelTest`: Content con `staleSince` cuando el repo es stale; Error solo sin caché.
- `lastUpdatedText`: < 1 min, redondeo (p. ej. 12 min 29 s -> 12, 12 min 30 s -> 13), diferencia negativa.
- UI (androidTest): `last_updated_banner` visible solo con `staleSince != null`; botones Pagar desactivados.
