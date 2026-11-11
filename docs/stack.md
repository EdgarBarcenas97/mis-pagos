# Stack: Android

## Tecnología

Kotlin · Jetpack Compose + Material 3 · Navigation Compose · ViewModel +
StateFlow · Coroutines · **Hilt** · **Room** · Retrofit + OkHttp +
kotlinx.serialization · Coil.
Tests: JUnit4 · kotlinx-coroutines-test · Turbine · **MockK** · MockWebServer · Compose UI Test.
Calidad: ktlint + **Android Lint**. CI: GitHub Actions (`scripts/verify.sh`).
CD: tag `vX.Y.Z` → APK firmado en GitHub Releases (`/publish-android`).
Un solo módulo `app`. Paquete raíz `dev.base.app`. Decisiones en `docs/adr/0001-stack.md`.

## Arquitectura (Clean + MVVM por paquetes)

```
app/src/main/java/dev/base/app/
├── core/
│   ├── common/        AppError, AppResult, DispatcherProvider
│   ├── database/      AppDatabase (Room): cada feature registra aquí su entidad y DAO
│   ├── di/            CoreModule (Hilt): dispatchers, Json, OkHttp, base de datos
│   ├── designsystem/  AppTheme y tokens (color, tipografía, espaciado)
│   └── network/       creación de Retrofit/OkHttp
└── feature/<x>/
    ├── data/          DTOs, entidades Room, DAOs, mappers, RepositoryImpl
    ├── di/            módulo Hilt: @Binds de la interfaz del dominio a su Impl
    ├── domain/        modelos, interfaz Repository, casos de uso
    └── presentation/  UiState, ViewModel, Screen
```

- Dependencias: `presentation → domain ← data`. `domain` no importa Android,
  red ni persistencia.
- DI con Hilt: `@HiltViewModel` + `@Inject constructor`; las pantallas obtienen
  el ViewModel con `hiltViewModel()`. Cada feature tiene su módulo en `di/`.
- Room: el esquema se exporta a `app/schemas/` (se versiona en git).
- **Feature de referencia:** `feature/home/`. Copia sus patrones.

## Convenciones

- ViewModel expone `StateFlow<XUiState>`; `XUiState` es una `sealed interface`
  (Loading, Content, Empty, Error) o un data class con estado explícito.
- Composables de pantalla: `XRoute` (conecta el ViewModel) + `XScreen`
  (sin estado, recibe datos y lambdas; es lo que se testea y previsualiza).
- Nada de colores, tamaños ni tipografías hardcodeados: usa `AppTheme` y `Spacing`.
- Textos visibles en `strings.xml`. testTags como constantes en el archivo de
  la pantalla (`object HomeTags`).
- Errores: las excepciones de red se convierten en `AppError` en `data`; la UI
  nunca ve excepciones.
- Corrutinas: inyecta `DispatcherProvider`; nada de `Dispatchers.IO` directo.

## Tests

| Tipo | Ruta | Comando |
|---|---|---|
| Unitarios (ViewModel, repositorio, mappers) | `app/src/test/` | `./gradlew testDebugUnitTest` |
| UI (Compose) | `app/src/androidTest/` | `./gradlew connectedDebugAndroidTest` (requiere emulador) |

- Mínimo por feature: ViewModel (todos los estados), repositorio (éxito y
  error) y un test de UI de humo por pantalla.
- Cada test lleva `// Cubre: <feature>/R<n>`.
- Fakes antes que mocks; MockK solo en fronteras (DAO, reloj); MockWebServer para red.

## Comandos

- `scripts/verify.sh` → ktlint + Android Lint + tests unitarios + compila la app
  y los tests de UI.
- `scripts/verify.sh --ui` → además, los tests de UI en el emulador.
- `/compile-app` (o `scripts/run-app.sh`) → enciende el emulador si hace falta,
  instala y abre la app. `--screenshot <png>` guarda una captura.

## CI/CD

| Evento | Workflow | Qué corre |
|---|---|---|
| PR y push a `main` | `android-ci.yml` | `init.sh --quick` + `verify.sh`; sube el APK de debug como artefacto |
| Tag `vX.Y.Z` (`/publish-android`) | `android-release.yml` | `verify.sh` + APK de release firmado → GitHub Release |

- `versionName` sale del tag y `versionCode` del número de ejecución del
  workflow. No se editan a mano.
- Firma: secrets `ANDROID_KEYSTORE_*`. Los crea el humano una vez con
  `scripts/setup-signing.sh` en su terminal; la llave vive en
  `~/.android-keystores/`, nunca en el repo.

## Demo con el MCP `mobile`

Con el emulador encendido, el MCP permite listar dispositivos, abrir la app
(`dev.base.app`), tocar elementos, leer la pantalla y tomar capturas. En la
fase 6 de `/feature`, recorre cada criterio de aceptación y guarda las capturas
en `progress/evidence/<feature>/`.
