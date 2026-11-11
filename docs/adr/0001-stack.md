# ADR 0001: Stack de la app Android

- **Estado:** Aceptado
- **Fecha:** 2026-11-11
- **Contexto de uso:** retos técnicos en vivo de 90 minutos y proyectos que
  arrancan desde esta base.

## Contexto

La base tiene que compilar a la primera, ser familiar para un equipo Android
actual y no obligar a elegir versiones en vivo. Cada decisión salió del
cuestionario de stack de `/harness`. Las versiones se validaron juntas en la
plantilla antes de usarla.

## Decisiones

| Tema | Elegido | Alternativas descartadas | Por qué |
|---|---|---|---|
| UI | Jetpack Compose + Material 3 | Views/XML | Estándar actual; previews y tests de UI más simples |
| DI | **Hilt** | DI manual (AppContainer), Koin | Es lo que la mayoría de equipos Android espera. Como va preconfigurado, su costo de setup no se paga en vivo |
| Persistencia | **Room** (con KSP) | Ninguna, solo DataStore | Muchos retos piden caché u offline-first; Room queda listo con el esquema exportado |
| Red | **Retrofit + OkHttp + kotlinx.serialization** | Ktor Client | Estándar en Android nativo; `safeApiCall` ya convierte errores en `AppError` y tiene tests |
| Tests | **Fakes + MockK** | Solo fakes, MockK para todo | Fakes como regla, porque los tests son claros. MockK solo en fronteras (DAO, reloj, red) |
| Módulos | **Un módulo `:app`** con capas por paquete | Multimódulo con convention plugins | Compila más rápido y se navega fácil en 90 min. Se pasa a multimódulo cuando haya varios equipos o el build lo pida |
| Análisis | **ktlint + Android Lint** | detekt, solo ktlint | Android Lint detecta problemas propios de Android (accesibilidad, recursos, APIs) sin dependencias extra |
| CI | **GitHub Actions** que corre `scripts/verify.sh` | Sin CI, CI con emulador | El mismo gate local y remoto; los tests en emulador en CI son lentos y frágiles |
| CD | **Tag `vX.Y.Z` → APK firmado en GitHub Releases** (`/publish-android`) | Release automático en cada merge; Firebase App Distribution; Play Console | Publicar es una decisión humana. GitHub Releases no pide cuentas extra; la versión sale del tag, sin commits de bump |
| Tests de UI | **Compose UI Test** | + screenshot tests (Roborazzi) | Suficiente para verificar comportamiento; los screenshots exigen mantener imágenes de referencia |

## Arquitectura

Clean + MVVM por paquetes: `core/` + `feature/<x>/{data,domain,presentation,di}`.
`presentation → domain ← data`: el dominio no depende de Android, red ni
persistencia.

## Consecuencias

- `verify.sh` corre ktlint, Android Lint, tests unitarios y compila la app y
  los tests de UI. Es la definición de "terminado".
- Subir versiones se hace en harness-kit con `verify` en verde, nunca en vivo.
- Sin multimódulo ni detekt: si el proyecto crece, se revisa este ADR.
