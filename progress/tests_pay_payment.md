# Tests: pay_payment (fase red)

Verificado: `testDebugUnitTest` compila (120 tests, 69 fallan; **los 48 de payments_list pasan**);
`compileDebugUnitTestKotlin assembleDebugAndroidTest ktlintCheck` en verde; `scripts/trace.sh pay_payment` R1–R15 OK.
`PayScreenTest` solo compilado (sin emulador).

## Mapa R → tests (archivos en app/src/test/.../feature/payments/ salvo indicación)
| R | Tests |
|---|---|
| R1 | PayViewModelTest (Pendiente/Vencido abren, Pagado/EnProceso/Unknown no, Pagado local no reabre, Verificando no abre); PayScreenTest (botón solo p1/p2, click) |
| R2 | PayViewModelTest (onCancel sin llamada/llave/save; onConfirm sin diálogo); PayScreenTest (textos exactos, callbacks, sin diálogo) |
| R3 | PayViewModelTest (una llamada, llave k1, Processing, diálogo cerrado; llave distinta por pago); PaymentGatewayImplTest (POST path + header); PayScreenTest (Procesando… deshabilitado) |
| R4 | PayViewModelTest (doble confirm/doble tap en Processing, pay en Verifying, llave guardada) ; PayScreenTest |
| R5 | PayViewModelTest (Paid + PayMessage.Paid, onMessageShown); PaymentGatewayImplTest 200 PAGADO; PayScreenTest (PayState.Paid pinta Pagado sin botón) |
| R6 | PayViewModelTest (Unknown → Verifying attemptsLeft>0); PaymentGatewayImplTest (503/500, desconexión, timeout, 200 no PAGADO, JSON inválido → Unknown); PayScreenTest |
| R7 | PayViewModelTest (reintentos a 3 s exactos, 3 veces, misma llave; reintento Paid para); FakeBackendInterceptorPayTest (misma llave tras timeout); PayEndToEndTest |
| R8 | PayViewModelTest (Verifying(0), llave conservada; onVerifyAgain misma llave → Paid); PayScreenTest |
| R9 | PayViewModelTest (Declined: Listo, aviso, llave borrada, nueva llave; Declined en reintento); PaymentGatewayImplTest (422/400/409/404); interceptor 422 |
| R10 | PayViewModelTest (recarga PAGADO resuelve y para reintentos; recarga no borra Processing/Verifying/Paid); FakePaymentsServerTest + interceptor GET refleja PAGADO |
| R11 | FakePaymentsServerTest (JSON exacto, idempotencia llave/pago, concurrencia, p3 sin cargo); FakeBackendInterceptorPayTest (latencia 2–5 s y inyectable, 200, 422, timeout con/sin registro, tasas por defecto, ya pagado) |
| R12 | PayEndToEndTest (timeout que registra → éxito ⇒ Paid y chargeCount==1; sin registro; 3 timeouts) |
| R13 | PayScreenTest `chipDeEstadoNoEsPulsableNiBoton` (assertHasNoClickAction + sin Role) |
| R14 | PayViewModelTest (save antes de pay por orden de eventos y snapshot; Unknown no borra; Paid/Declined borran después); PrefsPendingPaymentStoreTest (commit, no apply, archivo pending_payments) |
| R15 | PayViewModelTest (llave previa → Verifying y reintentos con esa llave, sin llaves nuevas; confirma → Paid y se borra); PrefsPendingPaymentStoreTest (persistencia entre instancias); PayEndToEndTest (app cerrada tras cobro) |

## Por qué fallan
- Casi todos: `NotImplementedError` (TODO en PayViewModel, PaymentGatewayImpl, FakePaymentsServer, FakeBackendInterceptor.pay, PrefsPendingPaymentStore).
- 5 de FakeBackendInterceptorPayTest: `assertThrows(SocketTimeoutException)` recibe `NotImplementedError` (mismo motivo).
- 2 de PayViewModelTest (R15) fallan por aserción: init aún no lee el store (TODO T3).
- Pasan ya (son regresión, no rojo): FakePaymentsServerTest "paymentsJson exacto" ×2 (contrato devuelve initialJson) y PrefsPendingPaymentStoreTest "usa el archivo pending_payments".

## Interpretaciones / huecos de la spec
1. Se asume que el diálogo se cierra al confirmar (`confirming == null` tras onConfirm).
2. "sin un pago en curso" (R1): se asume por id; no se testea si pagar p2 está bloqueado mientras p1 procesa.
3. Valores intermedios de `attemptsLeft` no especificados: solo se exige >0 tras el primer Unknown y 0 al agotar.
4. R15: no está definido si el primer reintento es inmediato o a los 3 s; los tests aceptan ambos.
5. Qué pasa tras onVerifyAgain que vuelve a dar Unknown (¿3 reintentos más?) no está definido; no se testea.
6. Interceptor: orden de sorteos no definido; los tests usan tasas 0/1 y azar constante. Falta definir el header ausente y un id desconocido.
7. Pago ya pagado + decline/timeout forzado: no está definido cuál gana; solo se testea con tasas 0.
8. R13 color verde del chip y el snackbar (vive en PaymentsRoute, no en PaymentsScreen) no son testeables en PaymentsScreen: verificar en la demo.
9. Reusar la misma llave para otro pago en el servidor: no definido.
