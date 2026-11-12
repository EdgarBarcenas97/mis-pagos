# Demo `pay_payment` — 2026-11-11 · emulador Pixel_API_34_Lite (API 34)

APK: `~/Desktop/mis-pagos.apk` (= `main` 4dcc011). Mockup: https://claude.ai/artifact/JdDrU7CGZ8uvQd6WTMFc5X.
Recorrido con `adb` + `uiautomator` (textos leídos del árbol de UI, no solo de la imagen).

| Criterio / estado | Captura | Marco del mockup | ¿Coincide? | Diferencias |
|---|---|---|---|---|
| R1 Pagar solo en Pendiente y Vencido | `demo-04-verificando.png` | Contenido | Sí | — |
| R2 Diálogo "Confirmar pago" con monto y fecha | `demo-01-confirmar.png` | Confirmar pago | Sí (textos exactos) | Fondo del diálogo lavanda (M3 por defecto), el mockup lo pide blanco |
| R3 Procesando… | `demo-02-procesando.png` | Procesando | Sí | — |
| R4 Doble tap sobre "Procesando…" | — | — | Sí | 2 taps extra durante Procesando: ningún segundo cobro, el pago terminó en un solo estado |
| R5 Pagado | `demo-03-resultado.png` | Pago registrado | Sí | "Vence" → "Venció el 30 nov 2026" + chip Pagado. El snackbar no se capturó (dura menos que el volcado de UI) |
| R6 Timeout → Verificando, sin Pagar | `demo-04-verificando.png` | Verificando | Sí (textos exactos) | Sin el fondo ámbar del mockup (texto sobre la card) |
| R14 Llave en disco antes de la respuesta | — | — | Sí | `shared_prefs/pending_payments.xml` = `p1 → ddbc699f-…` tras cerrar la app en Procesando |
| R15 Reabrir → Verificando con la misma llave → Pagado | `demo-05-reabrir.png`, `demo-06-resuelto.png` | Verificando | Sí | Al terminar, la llave se borra del disco |
| R8 Verificar de nuevo | — | Verificar de nuevo | Por test | Requiere 4 timeouts seguidos; cubierto por `PayViewModelTest` |
| R9 Rechazo | — | Rechazado | Por test | `payDeclineRate` es 0 en la app |

Notas: el servidor simulado vive en memoria, así que al cerrar la app "olvida" los pagos; por eso al reabrir la lista vuelve a decir Pendiente hasta que la verificación concluye.
