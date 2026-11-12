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

## Decisiones del PO (2026-11-11)
1. **`EN_PROCESO`** se muestra como "En proceso", sin botón Pagar. Un estado desconocido
   se muestra sin botón y nunca rompe la pantalla.
2. **`VENCIDO`** se puede pagar.
3. **Timeout en `pay`**: el pago queda en "Verificando tu pago…" hasta conciliar con el
   servidor. El reintento usa la misma llave de idempotencia, nunca una nueva.
4. **Fecha límite**: al PO le da igual; se muestra la fecha de calendario (30 nov 2026),
   sin conversión de zona.
5. **Orden**: por fecha límite ascendente.

## Preguntas abiertas
- **Pago parcial**: sin respuesta del PO. Supuesto: no, siempre el monto completo de la cuota.
