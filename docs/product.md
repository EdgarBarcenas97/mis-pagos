# Producto: "Mis pagos"

## Visión
Que el cliente de un crédito vea sus pagos con montos y fechas correctos y pueda
pagar desde la app con la certeza de que **nunca se cobra dos veces**.

## Usuario y problema
Cliente de una fintech con un crédito activo (p. ej. `CR-88213`). Necesita saber
cuánto debe, para cuándo y qué ya pagó, y pagar sin miedo a errores. Es dinero
real: la confianza depende de que montos, fechas y estados sean exactos.

## Acción principal
Ver la lista de pagos del crédito y pulsar **"Pagar"** en uno pendiente.

## Fuente de datos
Backend **simulado** (fake en código, sin red real):
- `getPayments()` → JSON del enunciado; tarda ~1.5 s y falla el 30% de las veces.
- `pay(paymentId)` → tarda 2–5 s; a veces **timeout**, y en algunos timeouts el
  servidor **sí registró el pago**.

El JSON trae un estado fuera del enum pedido: `p4` = `EN_PROCESO`.

## Restricciones
- Sesión de 45 min (unos 20 de construcción): una feature núcleo completa vale
  más que tres a medias.
- Kotlin + Compose, desde cero sobre la plantilla del harness (`docs/stack.md`).
- UI en español (es-MX), moneda MXN.
- Montos con precisión decimal exacta (nunca `Double` para dinero).

## Criterio de éxito
1. La lista muestra monto, fecha límite y estado de cada pago.
2. Se ven los cuatro estados de UI: cargando, error con "Reintentar", vacío y contenido.
3. "Pagar" no duplica un pago: ni con doble tap, ni al reintentar tras un
   timeout en el que el servidor sí cobró. Hay tests que lo demuestran.
4. La app compila, `scripts/verify.sh` en verde, y existen `DECISIONS.md` (≤ 5
   líneas) y este archivo de reglas.

## Features (prioridad)
1. `payments_list`: lista y estados de UI.
2. `pay_payment`: pagar sin duplicar.
3. `offline_last_known` *(stretch)*: último dato bueno + "Actualizado hace X min".

## Fuera de alcance
- Backend real, autenticación y selección entre varios créditos.
- Pagos parciales o adelantados, y elegir método de pago.
- Recibos o comprobantes descargables, y notificaciones push.
- Tests de UI instrumentados en CI (ver `docs/adr/0001-stack.md`).

## Preguntas abiertas (para el PO): supuesto por defecto entre paréntesis
1. **`EN_PROCESO`**: ¿cómo se muestra? *(Etiqueta "En proceso", sin botón Pagar.
   Cualquier estado desconocido se muestra sin botón y nunca rompe la pantalla.)*
2. **`VENCIDO`**: ¿también se puede pagar? *(Sí: se sigue debiendo.)*
3. **Timeout en `pay`**: ¿qué ve el cliente? *(El pago queda "Verificando pago…",
   se concilia con el servidor y nunca se reintenta con una llave nueva. El fake
   acepta una llave de idempotencia por pago.)*
4. **Fecha límite**: `2026-11-30T00:00:00Z` en hora de CDMX cae el 29. *(Se trata
   como fecha de calendario: se muestra "30 nov 2026".)*
5. **Orden de la lista** *(por fecha límite ascendente)*.
6. **Pago parcial**: *(no, siempre el monto completo de la cuota)*.
