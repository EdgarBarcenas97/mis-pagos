# contract: payments_list

Estado: ok. `./gradlew --console=plain --quiet assembleDebug assembleDebugAndroidTest ktlintCheck` pasa (EXIT 0).

## Creado (`app/src/main/java/dev/base/app/feature/payments/`)
- `domain/Payment.kt`: `Payment`, `PaymentStatus` (Pending/Overdue/Paid/InProcess/Unknown, `isPayable` y `fromApi` con `TODO()`), `Loan`, `PaymentsRepository`.
- `data/PaymentsApi.kt`: `PaymentsApi` (`@GET("loans/payments")`), `LoanDto`, `PaymentDto` (`amount` con `BigDecimalSerializer`).
- `data/BigDecimalSerializer.kt`: `descriptor` real (`PrimitiveSerialDescriptor` STRING); `serialize`/`deserialize` con `TODO()`.
- `data/PaymentsMapper.kt`: `LoanDto.toDomain()` con `TODO()`.
- `data/FakeBackendInterceptor.kt`: constructor según design, `intercept` con `TODO()`, `SAMPLE_JSON` con el JSON exacto del enunciado (p1..p4, CR-88213).
- `data/PaymentsRepositoryImpl.kt`: `getPayments()` con `TODO()`.
- `di/PaymentsModule.kt` (real): `@Binds PaymentsRepository`; `@Provides @Singleton PaymentsApi` = `Network.retrofit("https://api.mispagos.fake/", client.newBuilder().addInterceptor(FakeBackendInterceptor()).build())`.
- `presentation/PaymentsFormat.kt`: `formatAmount`, `formatDueDate` con `TODO()`.
- `presentation/PaymentsViewModel.kt`: `PaymentsUiState` completo; `uiState` = `MutableStateFlow(Loading)` (sin carga en init todavía); `retry()` con `TODO()`.
- `presentation/PaymentsScreen.kt`: `PaymentsTags` completo; `PaymentsRoute` conecta el ViewModel (igual que HomeRoute); `PaymentsScreen` con `TODO()`.

## Modificado
- `MainActivity.kt`: `PaymentsDestination` como inicio -> `PaymentsRoute()`.
- `res/values/strings.xml`: `payments_title`, `payments_loan`, `payments_error`, `payments_empty`, `status_*`; se quitó `home_title`.
- `core/di/CoreModule.kt`: se eliminó `database()` y sus imports.

## Eliminado (D5)
- `feature/home/**`, `app/src/test/.../feature/home/**`, `app/src/androidTest/.../feature/home/**`, `core/database/AppDatabase.kt`.
- Quitar Room no rompió KSP ni el plugin Room (dependencias y plugin siguen en `app/build.gradle.kts`).

## Notas
- El JSON del enunciado no estaba en `docs/product.md` ni en `design.md`; se tomó literal del enunciado original de la sesión (coincide con el mockup `docs/design/mockup.html`).
- Desviación de forma: los parámetros de constructor y de data class no llevan trailing comma (estilo del repo). `PaymentsRepositoryImpl` declara el constructor en una línea porque ktlint (`class-signature`) lo exige.
- `app/schemas/dev.base.app.core.database.AppDatabase/1.json` queda huérfano (no se tocó; no estaba en D5). Decidir si se borra.
- `strings.xml` `state_empty` ya no se usa (aviso de Lint `UnusedResources`, no error).
- `PaymentsScreen` es `TODO()`: la app compila pero se cae al abrir hasta la fase 4.
- `.harness/phase` (modificado por el orquestador) no se incluyó en el commit.
