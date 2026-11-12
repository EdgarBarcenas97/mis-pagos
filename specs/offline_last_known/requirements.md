# offline_last_known (stretch, sin SDD completo)

> Hecha al final de la sesión por tiempo, fuera del flujo SDD (sin tests en rojo previos).
> Reporte: `progress/impl_offline.md`.

## R1
SI la carga de pagos falla y hay un último dato bueno guardado ENTONCES el sistema DEBE mostrar esa lista con el banner "Sin conexión · Actualizado hace X min" (`last_updated_banner`).

## R2
SI la carga falla y no hay dato guardado ENTONCES el sistema DEBE mostrar el estado de error (comportamiento de `payments_list`).

## R3
MIENTRAS se muestra el último dato bueno, el sistema DEBE desactivar "Pagar".

## R4
El último dato bueno DEBE sobrevivir a cerrar la app (SharedPreferences) y la antigüedad DEBE calcularse con un reloj inyectable.

## Fuera de alcance
- Pagar sin conexión (encolar pagos).
