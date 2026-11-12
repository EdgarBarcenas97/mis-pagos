# Demo `payments_list` — 2026-11-11 · emulador Pixel_API_34_Lite (API 34)

Mockup: https://claude.ai/artifact/JdDrU7CGZ8uvQd6WTMFc5X. Recorrido con `adb` (el MCP `mobile` no estaba cargado en esta sesión).

| Criterio / estado | Captura | Marco del mockup | ¿Coincide? | Diferencias |
|---|---|---|---|---|
| Splash (feature `splash`) | `01-splash.png` | — (no estaba en el mockup) | n/a | Pedido a mitad de sesión |
| R1 Cargando | `02-loading.png` | Cargando | Sí | El spinner se ve pequeño en la captura reducida |
| R2 Error + Reintentar | `04-error.png` | Error | Sí | Texto y botón exactos. Sin subtítulo del crédito (aún no se conoce el loanId) |
| R4 Vacío | — (solo tests) | Vacío | Por test | El backend simulado siempre trae 4 pagos |
| R5 Orden asc. + `Crédito CR-88213` | `03-content.png` | Contenido | Sí | p3, p2, p1, p4 |
| R6 `$1,250.10 MXN` · `$980.55 MXN` | `03-content.png` | Contenido | Sí | — |
| R7 `Vence el 30 nov 2026` / `Venció el 30 sep 2026` | `03-content.png` | Contenido | Sí | — |
| R8/R9 chips de estado | `03-content.png` | Contenido | **Parcial** | Chips con borde (`AssistChip`) en vez de relleno tonal. "Pagado" en gris (`secondary`), no en verde. "Pendiente" en el verde primario del tema, no en azul. Desviación pendiente de decisión del humano |
| R3 Reintentar vuelve a cargar | — | Error → Cargando | Por test | Cubierto por `PaymentsViewModelTest` |
