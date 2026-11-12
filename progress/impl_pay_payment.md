# impl pay_payment

Estado: `testDebugUnitTest` verde (120, incluidos los 48 de payments_list) y `scripts/verify.sh` -> [OK]. No se tocó ningún test.
PayScreenTest (androidTest) solo compila; no se corrió porque no hay emulador.

## Qué se hizo
- T1 `FakePaymentsServer`: llaves usadas, cargos por pago (máx. 1), los PAGADO del JSON inicial cuentan como pagados.
  `paymentsJson()` devuelve initialJson sin cambios mientras no haya cargos; con cargos, reescribe `status` con regex.
- T1 `FakeBackendInterceptor.pay`: duerme `nextLong(payLatencyMs)`, luego sorteos en este orden: decline (422) -> timeout
  (registra si `nextDouble() < registeredOnTimeoutRate`, lanza SocketTimeoutException) -> registra + 200 PAGADO.
  Un pago ya pagado no genera cargo nuevo (`registerPayment` es idempotente). Primero devolvía 200 de inmediato
  para pagos ya pagados, sin sortear; eso rompía el e2e "varios timeouts que registran", que espera que los timeouts
  sigan pasando. Lo cambié para que siempre sortee primero.
- T2 `PaymentGatewayImpl` (safeApiCall -> PayResult según design) y `PrefsPendingPaymentStore` (`commit()`).
- T3 `PayViewModel`: la llave se guarda antes de llamar a pay (R14). Unknown -> Verifying(3) y luego 3 reintentos
  cada 3 s con la misma llave, hasta Verifying(0). `onVerifyAgain` reintenta de inmediato y vuelve a hacer 3 reintentos.
  Con Paid/Declined se borra la llave. `onPaymentsLoaded` con PAGADO cancela la verificación y lo marca Paid
  (también pone el mensaje Paid). En init (R15) cada llave pendiente pasa a Verifying con un intento inmediato.
- T4 UI: botón Pagar (pay_button_<id>) solo en filas pagables y sin pago en curso, Procesando… deshabilitado, diálogo
  de confirmación, bloque Verificando + "Verificar de nuevo" con 0 intentos, snackbar en PaymentsRoute. El chip es un
  Surface tonal sin acción; Pagado usa `AppColors.current.success/onSuccess` (relleno sólido, según el coordinador).
  No toqué Theme.kt (el punto 5 se canceló).

## Desviaciones / avisos
- Diálogo: la fecha sale de `formatDueDate(..).removePrefix("Vence el ")` y el monto sin moneda de
  `formatAmount(..).removeSuffix(" MXN")`, así no hubo que tocar PaymentsFormat.
- **Commits mezclados:** el otro agente (offline_last_known) hizo commit de `PaymentsScreen.kt` en 14584cd con mi UI de
  T4 adentro (los dos editábamos el archivo al mismo tiempo). Por eso la UI de pay_payment quedó en ese commit y no en
  los míos (2db87d3 data+VM, 71d7ac5 ktlint).
- En PaymentsScreen hay un comentario del otro agente: "con staleSince != null, Pagar debe ir desactivado". No está
  implementado: es alcance de offline_last_known.
