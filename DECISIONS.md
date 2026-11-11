1. **Fuente de verdad:** un fake con latencia y fallos detrás de `PaymentsRepository`. Se cambia por Retrofit sin tocar la UI, pero hoy no se prueba red real.
2. **Dinero y fechas exactos:** `BigDecimal` para montos y `LocalDate` tomada del `Z` sin convertir la zona horaria. Es más verboso que `Double`/`Instant`, pero no hay redondeos ni vencimientos que se adelanten un día.
3. **Estado de UI:** `sealed interface` (Loading, Empty, Content, Error) expuesto como `StateFlow` desde el ViewModel. Es simple y testeable, aunque cada fila de pago necesita su propio sub-estado.
4. **Sin pagos duplicados:** una llave de idempotencia por pago que se reutiliza en cada reintento, botón bloqueado mientras procesa, y tras un timeout se muestra "Verificando" y se concilia. Cuesta más estados, pero un timeout nunca termina en un segundo cobro.
5. **Fuera por tiempo:** caché offline con Room (stretch), Retrofit/MockWebServer y tests de UI en CI. Lo que se entrega queda cubierto por tests unitarios.
