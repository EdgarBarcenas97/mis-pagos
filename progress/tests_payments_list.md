# tests: payments_list

Estado: ok (rojo por la razón correcta).

- `./gradlew testDebugUnitTest`: compila; 48 tests, 43 fallan (los 43 de payments_list), 5 pasan (`SafeApiCallTest`, ya existente).
- `assembleDebugAndroidTest` y `ktlintCheck`: EXIT 0.
- `scripts/trace.sh payments_list`: R1–R10 OK, sin huérfanos.
- `PaymentsScreenTest` (androidTest) solo se compiló; no se corrió (no hace falta emulador en esta fase).

## Archivos

Unitarios en `app/src/test/java/dev/base/app/feature/payments/`:
- `data/PaymentsMapperTest.kt` (12)
- `data/FakeBackendInterceptorTest.kt` (8)
- `data/PaymentsRepositoryImplTest.kt` (9)
- `presentation/PaymentsViewModelTest.kt` (7)
- `presentation/PaymentsFormatTest.kt` (7)

UI: `app/src/androidTest/java/dev/base/app/feature/payments/presentation/PaymentsScreenTest.kt` (8)

## Mapa R → test (y por qué falla)

| R | Tests | Por qué falla hoy |
|---|---|---|
| R1 | VM `al abrir muestra Loading mientras pide los pagos` (repo bloqueado con un gate: sigue en Loading y `calls == 1`); UI `loadingMuestraIndicadorDeCarga` | aserción `expected:<1> but was:<0>`: el VM no carga en init |
| R2 | VM `fallo de red…`, `fallo del servidor 503…`; Repo `503 es Server 503`, `404 es NotFound`, `desconexion es Network` (DISCONNECT_AT_START), `JSON invalido es Serialization`, `fecha invalida es Serialization`; UI `errorMuestraTextoYReintentar` | VM: aserción (`Loading` en vez de `Error`); Repo: `TODO()` en `getPayments` |
| R3 | VM `retry vuelve a Loading y pide los pagos de nuevo` (gate: Loading y `calls == 2`, luego Content), `retry que vuelve a fallar termina en Error`; UI `pulsarReintentarLlamaOnRetry` | aserción (no hay carga en init) / `TODO()` en `retry()` |
| R4 | VM `respuesta sin pagos muestra Empty`; Repo `200 sin pagos…`; UI `emptyMuestraTextoDeVacio` | aserción / `TODO()` |
| R5 | Repo `200 devuelve… ordenados` (p3,p2,p1,p4; loanId CR-88213; path `/loans/payments`), `200 mapea monto, fecha y estado`; VM `pagos recibidos se muestran como Content…`; UI `contentMuestraListaEnOrden…` (orden + "Crédito CR-88213") | `TODO()` / aserción |
| R6 | Mapper `monto… BigDecimal exacto conservando la escala` (`BigDecimal("1250.10")`, scale 2, `980.55`), `monto con mas digitos de los que caben en un Double…`; Format `$1,250.10 MXN`, `$980.55 MXN`, siempre 2 decimales (`1250.1`, `1000`), independiente del Locale por defecto; UI `filaMuestraMontoYFechaFormateados` | `TODO()` en `BigDecimalSerializer.deserialize` y `formatAmount` |
| R7 | Mapper fecha UTC con TZ `America/Mexico_City` (restaurada en `finally`), `23:30Z` con TZ `Asia/Tokyo`, fecha inválida → `DateTimeParseException`; Format "Vence el 30 nov 2026", "Venció el 30 sep 2026", independiente del Locale; Repo fecha inválida → Serialization | `TODO()` (en el de fecha inválida: `NotImplementedError` en vez de `DateTimeParseException`) |
| R8 | Mapper `los cuatro estados…` (`fromApi`), `solo Pendiente y Vencido son pagables`, `el JSON del enunciado mapea cada pago a su estado`; UI `chipsMuestranLaEtiquetaDeCadaEstado` (incluye "En proceso" en `payment_status_p4`) | `TODO()` en `fromApi`/`isPayable` |
| R9 | Mapper `estado desconocido es Unknown… no pagable`, `cadena vacia es Unknown`, `…no descarta el resto de la lista`, `un estado desconocido en el JSON no rompe el parseo`; Repo `estado desconocido no descarta el resto`; UI `estadoDesconocidoMuestraEnRevision…` | `TODO()` |
| R10 | Interceptor: `failureRate 1` → 503, `0` → 200 con `SAMPLE_JSON`, body inyectado, `sleeper` registra `[1500]` (éxito y error), latencia inyectada, `Random` fijo 0.29 → 503 y 0.30/0.99 → 200 con la tasa por defecto; Repo 503; VM 503 | `TODO()` en `intercept` |

## Decisiones de test

- Fakes: `FakePaymentsRepository` (cola de resultados + contador + `CompletableDeferred` como gate), `TestDispatcherProvider`, `FixedRandom` (subclase de `kotlin.random.Random`). Sin MockK.
- El gate hace que los tests de Loading no dependan de si el VM pone `Loading` antes o dentro del `launch`.
- `desconexion es Network` encola 3 `DISCONNECT_AT_START` por si OkHttp reintenta (con la cola vacía MockWebServer bloquearía el test).
- Interceptor: `0.30` exacto debe responder 200 (contrato: falla si `nextDouble() < failureRate`).
- UI: textos con `hasText(..) or hasAnyDescendant(hasText(..))` sobre el tag, para no atar el test a cómo se fusiona la semántica.

## Huecos de la spec (no testeados, a decidir)

- Formato del día de un dígito ("5 ene" vs "05 ene") y abreviatura de septiembre: la spec fija "sep"; ojo, CLDR es-MX suele dar "sept." con `DateTimeFormatter`, así que la implementación probablemente necesite abreviaturas propias.
- Estados en minúsculas o con espacios (`"pendiente"`): no definido; hoy se espera `Unknown`, pero no se testea.
- Montos negativos o moneda distinta de MXN: no definidos.
- `retry()` desde Content/Empty: la spec solo lo define desde Error.
- Qué texto de error muestra la UI según el `AppError` (Network vs Server): R2 fija un único texto; el test de UI solo comprueba ese texto con `AppError.Network`.
