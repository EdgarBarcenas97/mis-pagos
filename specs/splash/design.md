# splash: diseño (retroactivo)

- `androidx.core:core-splashscreen:1.2.0` (verificada en Google Maven), `installSplashScreen()` en `MainActivity` antes de `super.onCreate`.
- `res/values/themes.xml` y `res/values-v31/themes.xml`: tema `Theme.App.Starting` con `@color/brand_green`; branding en v31.
- `res/drawable/ic_splash_logo.xml` (tarjeta de pago) y `res/drawable/splash_brand.xml` (texto trazado como vector).

## Alternativa descartada
- Activity de splash propia con retraso: es el antipatrón que la SplashScreen API reemplaza y añade espera artificial.

## Limitaciones conocidas
- "Mis pagos" es un trazado vectorial, no texto: no se traduce ni lo lee TalkBack.
