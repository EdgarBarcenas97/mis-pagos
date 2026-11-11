# pay_payment: pagar sin duplicar

Pantalla y estados: `docs/design.md` → "Mis pagos", sección *Estados de una fila al pagar*
(Listo, Confirmar, Procesando, Verificando, Pagado, Rechazado). Decisión del PO: un timeout
se muestra como "Verificando tu pago…" y el reintento usa la misma llave.

## R1
MIENTRAS un pago está en estado Pendiente o Vencido y sin un pago en curso, el sistema DEBE mostrar el botón "Pagar" (`pay_button_<id>`); en cualquier otro estado NO DEBE mostrarlo.

## R2
CUANDO el usuario pulsa "Pagar", el sistema DEBE mostrar el diálogo "Confirmar pago" con el texto "Vas a pagar $1,250.10 MXN de la cuota que vence el 30 nov 2026." y los botones "Pagar $1,250.10" (`pay_confirm`) y "Cancelar" (`pay_cancel`); "Cancelar" DEBE cerrarlo sin llamar al servidor.

## R3
CUANDO el usuario confirma, el sistema DEBE generar una llave de idempotencia nueva para ese pago, llamar a `pay` una sola vez con ella y mostrar el botón deshabilitado "Procesando…".

## R4
SI se pide pagar un pago que ya está en Procesando o Verificando (doble tap, segundo confirm) ENTONCES el sistema NO DEBE llamar otra vez al servidor.

## R5
CUANDO el servidor confirma el pago, el sistema DEBE mostrar el pago como "Pagado", sin botón, y el aviso "Pago registrado.".

## R6
SI `pay` termina en timeout, error de red o error 5xx ENTONCES el sistema DEBE mostrar en esa fila "Verificando tu pago…" y "No vuelvas a pagar; te avisaremos en cuanto se confirme." (`payment_verifying_<id>`), sin botón "Pagar".

## R7
MIENTRAS un pago está en Verificando, el sistema DEBE reintentar `pay` con la **misma** llave cada 3 s, hasta 3 veces; si alguna confirma, aplica R5.

## R8
SI se agotan los reintentos sin respuesta definitiva ENTONCES el sistema DEBE seguir en Verificando y mostrar el botón "Verificar de nuevo" (`payment_verify_again_<id>`), que reintenta con la misma llave.

## R9
SI el servidor rechaza el pago (HTTP 4xx) ENTONCES el sistema DEBE mostrar el aviso "No se pudo procesar el pago y no se hizo ningún cargo.", descartar la llave y volver a mostrar "Pagar".

## R10
CUANDO la lista se recarga y trae como PAGADO un pago que estaba en Verificando, el sistema DEBE darlo por pagado (R5); recargar la lista NO DEBE borrar el estado de un pago en curso.

## R11
El servidor simulado DEBE: tardar entre 2 y 5 s en `pay`; responder a veces timeout, y en una parte de esos timeouts registrar el pago; y ser idempotente por llave: el mismo `Idempotency-Key` o un pago ya pagado nunca generan un segundo cargo. El azar, las tasas y la espera DEBEN ser inyectables.

## R12
Con el servidor simulado real (timeout que sí registra en el primer intento y éxito después), el flujo completo ViewModel → gateway → Retrofit → interceptor DEBE terminar en "Pagado" con exactamente **1** cargo registrado.

## R13
El chip de estado DEBE ir relleno (tonal) como en el mockup, con "Pagado" en un verde propio del tema, y NO DEBE ser pulsable ni anunciarse como botón.

## Fuera de alcance
- Persistir la llave tras matar el proceso (vive en el ViewModel).
- Pagos parciales, elegir método de pago y comprobantes.
- Pagar sin conexión (feature `offline_last_known`).
