# app_logo: diseño (retroactivo)

- `res/mipmap-anydpi-v26/ic_launcher.xml` y `ic_launcher_round.xml` (adaptive-icon con background, foreground y monochrome).
- `res/drawable/ic_launcher_{background,foreground,monochrome}.xml`: foreground = `ic_splash_logo` reescalado a la zona segura de 66dp dentro de 108dp.
- `res/values/colors.xml`: `brand_green` = `#FF1F6F5C`, usado también por los temas del splash.

## Alternativa descartada
- PNG rasterizados por densidad: más archivos y peor nitidez; con minSdk 26 basta el ícono adaptativo vectorial.
