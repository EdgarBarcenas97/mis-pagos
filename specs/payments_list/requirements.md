# payments_list: lista de pagos con estados de UI

Pantalla y estados: `docs/design.md` → "Mis pagos", sección *Estados de pantalla*
(Cargando, Error, Vacío, Contenido) y *Fila de pago*.

## R1
CUANDO la pantalla "Mis pagos" se abre, el sistema DEBE mostrar el estado de carga mientras pide los pagos.

## R2
SI la petición de pagos falla ENTONCES el sistema DEBE mostrar el estado de error con el texto "No pudimos cargar tus pagos. Revisa tu conexión e inténtalo de nuevo." y el botón "Reintentar".

## R3
CUANDO el usuario pulsa "Reintentar", el sistema DEBE volver a mostrar el estado de carga y pedir los pagos de nuevo.

## R4
SI la respuesta no trae pagos ENTONCES el sistema DEBE mostrar el estado vacío con el texto "Aún no tienes pagos programados para este crédito.".

## R5
CUANDO los pagos llegan, el sistema DEBE mostrarlos ordenados por fecha límite ascendente, con el crédito (`Crédito CR-88213`) como subtítulo.

## R6
El sistema DEBE representar cada monto como `BigDecimal` leído del texto del JSON (sin pasar por `Double`) y mostrarlo como `$1,250.10 MXN`.

## R7
El sistema DEBE tomar la fecha límite como fecha de calendario del instante UTC (`2026-11-30T00:00:00Z` → 30 nov 2026), sin importar la zona horaria del dispositivo, y mostrarla como `Vence el 30 nov 2026` (o `Venció el …` si el pago está pagado).

## R8
El sistema DEBE mapear los estados `PENDIENTE`, `VENCIDO`, `PAGADO` y `EN_PROCESO` a las etiquetas "Pendiente", "Vencido", "Pagado" y "En proceso", y solo `PENDIENTE` y `VENCIDO` se consideran pagables.

## R9
SI llega un estado desconocido ENTONCES el sistema DEBE mostrar el pago con la etiqueta "En revisión", sin considerarlo pagable y sin descartar el resto de la lista.

## R10
El backend simulado `getPayments()` DEBE tardar ~1.5 s y fallar el 30% de las veces, con el azar y la latencia inyectables para que los tests sean deterministas.

## Fuera de alcance
- Botón "Pagar" y su flujo (feature `pay_payment`). Aquí solo se calcula `isPayable`.
- Caché y "Actualizado hace X min" (feature `offline_last_known`).
- Red real (Retrofit), pull-to-refresh y tests de UI instrumentados en CI.
