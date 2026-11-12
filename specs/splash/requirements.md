# splash (retroactiva)

> Pedida por el humano a mitad de sesión y hecha fuera del flujo SDD (`sdd: false`).
> Esta spec documenta lo construido (commit 6a104c2); evidencia en `progress/evidence/splash/splash.png`.

## R1
CUANDO la app arranca, el sistema DEBE mostrar un splash con el ícono de la app centrado sobre el verde de marca.

## R2
DONDE el dispositivo tenga API 31 o superior, el splash DEBE mostrar el texto "Mis pagos" como branding inferior.

## R3
El splash DEBE desaparecer cuando la app dibuja su primer frame, sin retraso artificial.

## Fuera de alcance
- Texto "Mis pagos" en API 26–30 (la librería compat no soporta branding).
- Variante de modo oscuro.
