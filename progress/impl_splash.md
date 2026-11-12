# impl: splash

Feature pequeña (`sdd: false`). Splash de arranque con la SplashScreen API.

## Qué hice
- Dependencia `androidx.core:core-splashscreen:1.2.0`:
  `gradle/libs.versions.toml` (`core-splashscreen`, `androidx-core-splashscreen`) y `app/build.gradle.kts`.
- `res/values/themes.xml`: `Theme.AppBase.Starting` (padre `Theme.SplashScreen`): fondo `#1F6F5C`
  (primary de `AppTheme` en modo claro), ícono `@drawable/ic_splash_logo`, `postSplashScreenTheme = Theme.AppBase`.
  Compatible con minSdk 26.
- `res/values-v31/themes.xml`: lo mismo más `android:windowSplashScreenBrandingImage = @drawable/splash_brand`
  (este atributo solo existe en la plataforma desde API 31; la librería compat no lo soporta).
- `res/drawable/ic_splash_logo.xml`: vector de 288dp con una tarjeta de pago blanca dentro del círculo seguro de 192dp.
- `res/drawable/splash_brand.xml`: vector de 200x80dp con "Mis pagos" dibujado con trazos (los vectores no admiten
  texto). `strings.xml`: se agregó `splash_brand` = "Mis pagos" (en la imagen no se usa como recurso).
- `AndroidManifest.xml`: `MainActivity` usa `@style/Theme.AppBase.Starting`.
- `MainActivity.kt`: `installSplashScreen()` antes de `super.onCreate`. No hay retraso artificial: el splash
  se va con el primer frame.

## Versión y fuente
- `1.2.0`: la última estable (`<release>1.2.0</release>`, lastUpdated 2025-11-05) según Google Maven:
  https://dl.google.com/android/maven2/androidx/core/core-splashscreen/maven-metadata.xml (consultado el 2026-11-11).

## Verificación
- `./gradlew --console=plain --quiet assembleDebug ktlintCheck lintDebug` pasó (exit 0).
  Hubo un aviso no fatal de la caché incremental de Kotlin ("Storage ... already registered"), probablemente
  porque el otro subagente compilaba al mismo tiempo.
- Avisos de lint (no son errores): `VectorRaster` en `ic_splash_logo` (288dp, que es el tamaño que pide la guía
  del splash) y `UnusedResources` en `splash_brand` (la clave se agregó por pedido y la imagen no la usa).
- Emulador (API 34): `installDebug`, la app se abrió y se tomó una captura del splash en
  `progress/evidence/splash/splash.png`. A los 4 s la app ya mostraba la pantalla de pagos.

## Decisiones y desviaciones
- La app no tenía ícono de launcher (no hay `mipmap` ni `android:icon`), así que no había "ícono de la app" que
  reutilizar. Creé `ic_splash_logo` para el splash y no agregué un ícono de launcher porque está fuera del alcance.
  Si se agrega uno, conviene que `windowSplashScreenAnimatedIcon` apunte a él.
- El fondo es fijo (`#1F6F5C`) en claro y en oscuro: no hay `colors.xml` permitido ni `values-night`, y el tema
  de Compose no está disponible antes del primer frame.
- En API 26-30 (compat) solo se ven el ícono y el fondo. El branding "Mis pagos" aparece desde API 31.
