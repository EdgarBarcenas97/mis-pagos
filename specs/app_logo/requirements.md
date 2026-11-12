# app_logo (retroactiva)

> Pedida por el humano y hecha fuera del flujo SDD (`sdd: false`).
> Esta spec documenta lo construido (commit dbe786b); evidencia en `progress/evidence/app_logo/launcher.png`.

## R1
El sistema DEBE declarar un ícono de launcher adaptativo (`android:icon` y `android:roundIcon`) con la tarjeta de pago blanca sobre el verde de marca.

## R2
El ícono DEBE incluir una capa monocroma para íconos temáticos (Android 13+).

## R3
El verde de marca DEBE definirse una sola vez (`@color/brand_green`) y usarse en el ícono y en el splash.

## Fuera de alcance
- Nombre visible de la app ("AppBase" → "Mis pagos"): se corrige en `pay_payment`.
