package dev.base.app.feature.payments.presentation

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.base.app.core.common.AppError
import dev.base.app.core.designsystem.AppTheme
import dev.base.app.core.designsystem.StateTags
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val payments = listOf(
        Payment("p3", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 9, 30), PaymentStatus.Paid),
        Payment("p2", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 10, 31), PaymentStatus.Overdue),
        Payment("p1", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 11, 30), PaymentStatus.Pending),
        Payment("p4", BigDecimal("980.55"), "MXN", LocalDate.of(2026, 12, 31), PaymentStatus.InProcess)
    )

    private fun setScreen(state: PaymentsUiState, onRetry: () -> Unit = {}) {
        compose.setContent { AppTheme { PaymentsScreen(state = state, onRetry = onRetry) } }
    }

    private fun shows(text: String): SemanticsMatcher = hasText(text) or hasAnyDescendant(hasText(text))

    private fun scrollTo(id: String) {
        compose.onNodeWithTag(PaymentsTags.LIST).performScrollToNode(hasTestTag(PaymentsTags.item(id)))
    }

    // Cubre: payments_list/R1
    @Test
    fun loadingMuestraIndicadorDeCarga() {
        setScreen(PaymentsUiState.Loading)

        compose.onNodeWithTag(StateTags.LOADING).assertIsDisplayed()
        compose.onNodeWithText("Mis pagos").assertIsDisplayed()
    }

    // Cubre: payments_list/R2
    @Test
    fun errorMuestraTextoYReintentar() {
        setScreen(PaymentsUiState.Error(AppError.Network))

        compose.onNodeWithTag(StateTags.ERROR).assertIsDisplayed()
        compose
            .onNodeWithText("No pudimos cargar tus pagos. Revisa tu conexión e inténtalo de nuevo.")
            .assertIsDisplayed()
        compose.onNodeWithTag(StateTags.RETRY).assertIsDisplayed()
        compose.onNodeWithText("Reintentar").assertIsDisplayed()
    }

    // Cubre: payments_list/R3
    @Test
    fun pulsarReintentarLlamaOnRetry() {
        var retries = 0
        setScreen(PaymentsUiState.Error(AppError.Server(503)), onRetry = { retries++ })

        compose.onNodeWithTag(StateTags.RETRY).performClick()

        assertEquals(1, retries)
    }

    // Cubre: payments_list/R4
    @Test
    fun emptyMuestraTextoDeVacio() {
        setScreen(PaymentsUiState.Empty)

        compose.onNodeWithTag(StateTags.EMPTY).assertIsDisplayed()
        compose.onNodeWithText("Aún no tienes pagos programados para este crédito.").assertIsDisplayed()
    }

    // Cubre: payments_list/R5
    @Test
    fun contentMuestraListaEnOrdenConElCreditoComoSubtitulo() {
        setScreen(PaymentsUiState.Content("CR-88213", payments))

        compose.onNodeWithTag(PaymentsTags.LIST).assertIsDisplayed()
        compose.onNodeWithText("Mis pagos").assertIsDisplayed()
        compose.onNodeWithText("Crédito CR-88213").assertIsDisplayed()
        listOf("p3", "p2", "p1", "p4").forEach { id ->
            scrollTo(id)
            compose.onNodeWithTag(PaymentsTags.item(id)).assertExists()
        }
        scrollTo("p3")
        val p3 = compose.onNodeWithTag(PaymentsTags.item("p3")).getBoundsInRoot().top
        val p2 = compose.onNodeWithTag(PaymentsTags.item("p2")).getBoundsInRoot().top
        assertTrue("p3 debe ir antes que p2", p3 < p2)
    }

    // Cubre: payments_list/R6, payments_list/R7
    @Test
    fun filaMuestraMontoYFechaFormateados() {
        setScreen(PaymentsUiState.Content("CR-88213", payments))

        scrollTo("p1")
        compose.onNodeWithTag(PaymentsTags.amount("p1")).assert(shows("$1,250.10 MXN"))
        compose.onNodeWithTag(PaymentsTags.due("p1")).assert(shows("Vence el 30 nov 2026"))
        scrollTo("p3")
        compose.onNodeWithTag(PaymentsTags.due("p3")).assert(shows("Venció el 30 sep 2026"))
        scrollTo("p4")
        compose.onNodeWithTag(PaymentsTags.amount("p4")).assert(shows("$980.55 MXN"))
    }

    // Cubre: payments_list/R8
    @Test
    fun chipsMuestranLaEtiquetaDeCadaEstado() {
        setScreen(PaymentsUiState.Content("CR-88213", payments))

        mapOf("p1" to "Pendiente", "p2" to "Vencido", "p3" to "Pagado", "p4" to "En proceso").forEach { (id, label) ->
            scrollTo(id)
            compose.onNodeWithTag(PaymentsTags.status(id)).assert(shows(label))
        }
    }

    // Cubre: payments_list/R9
    @Test
    fun estadoDesconocidoMuestraEnRevisionSinOcultarLosDemas() {
        val withUnknown = payments + Payment(
            "p5",
            BigDecimal("10.00"),
            "MXN",
            LocalDate.of(2027, 1, 31),
            PaymentStatus.Unknown("CONGELADO")
        )
        setScreen(PaymentsUiState.Content("CR-88213", withUnknown))

        scrollTo("p5")
        compose.onNodeWithTag(PaymentsTags.status("p5")).assert(shows("En revisión"))
        scrollTo("p3")
        compose.onNodeWithTag(PaymentsTags.item("p3")).assertExists()
    }
}
