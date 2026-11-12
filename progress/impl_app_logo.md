# impl app_logo

Feature pequeña (`sdd: false`): ícono adaptativo de launcher.

## Qué se hizo
- `res/values/colors.xml` (nuevo): `brand_green` = `#FF1F6F5C`.
- `res/values/themes.xml` y `res/values-v31/themes.xml`: `windowSplashScreenBackground` ahora usa `@color/brand_green` en lugar del hex repetido.
- `res/drawable/ic_launcher_background.xml`: `shape` sólido `@color/brand_green`.
- `res/drawable/ic_launcher_foreground.xml`: los mismos trazos de la tarjeta de `ic_splash_logo.xml` (viewport 288, círculo seguro de 192) en un `<group>` con escala 66/192 = 0.34375 alrededor de (144,144) y traslado -90, que deja el centro en (54,54) del lienzo de 108dp. Así el círculo seguro del splash coincide con la zona segura de 66dp.
- `res/drawable/ic_launcher_monochrome.xml`: la misma geometría en negro opaco (en la capa monocroma solo cuenta el alfa).
- `res/mipmap-anydpi-v26/ic_launcher.xml` e `ic_launcher_round.xml`: `<adaptive-icon>` con background, foreground y monochrome.
- `AndroidManifest.xml`: `android:icon="@mipmap/ic_launcher"` y `android:roundIcon="@mipmap/ic_launcher_round"` en `<application>`.
- `res/values/strings.xml`: eliminada `splash_brand` (no hay referencias `@string/splash_brand` ni `R.string.splash_brand`; solo la menciona un comentario en `drawable/splash_brand.xml`, que queda fuera de las rutas permitidas y no se tocó, así que ese comentario quedó desactualizado).

## Verificación
- `./gradlew --console=plain --quiet assembleDebug ktlintCheck lintDebug` -> exit 0.
- Lint: 4 warnings (sin errores). 3 ya existían (`VectorRaster` en ic_splash_logo, `PrivateResource` y `UnusedResources` en `state_empty`). La nueva es `ObsoleteSdkInt` por la carpeta `mipmap-anydpi-v26` (minSdk 26).
  Probé moverla a `mipmap-anydpi`, pero AAPT falló (`resource mipmap/ic_launcher not found`), así que se mantiene `-v26`, que es la ruta que pidió el humano.
- Emulador `emulator-5554`: `installDebug` OK. Captura del cajón de apps en `progress/evidence/app_logo/launcher.png`: "AppBase" muestra la tarjeta blanca sobre el verde de marca. En la pantalla de inicio el ícono no aparecía con su etiqueta, por eso se usó el cajón.

## Desviaciones
- Ninguna sobre lo pedido. No se tocó `feature/payments/**`, Gradle ni `.harness/phase`.
