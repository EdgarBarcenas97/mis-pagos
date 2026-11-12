package dev.base.app.feature.payments.presentation

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.base.app.core.designsystem.AppTheme
import dev.base.app.feature.payments.domain.Payment
import dev.base.app.feature.payments.domain.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PayScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val p1 = Payment("p1", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 11, 30), PaymentStatus.Pending)
    private val p2 = Payment("p2", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 10, 31), PaymentStatus.Overdue)
    private val p3 = Payment("p3", BigDecimal("1250.10"), "MXN", LocalDate.of(2026, 9, 30), PaymentStatus.Paid)
    private val p4 = Payment("p4", BigDecimal("980.55"), "MXN", LocalDate.of(2026, 12, 31), PaymentStatus.InProcess)
    private val content = PaymentsUiState.Content("CR-88213", listOf(p3, p2, p1, p4))

    private val payClicks = mutableListOf<Payment>()
    private var confirms = 0
    private var cancels = 0
    private val verifyAgain = mutableListOf<String>()

    private fun setScreen(payState: PayUiState) {
        compose.setContent {
            AppTheme {
                PaymentsScreen(
                    state = content,
                    onRetry = {},
                    payState = payState,
                    onPayClick = { payClicks += it },
                    onConfirm = { confirms++ },
                    onCancel = { cancels++ },
                    onVerifyAgain = { verifyAgain += it }
                )
            }
        }
    }

    private fun shows(text: String): SemanticsMatcher = hasText(text) or hasAnyDescendant(hasText(text))

    private fun scrollTo(id: String) {
        compose.onNodeWithTag(PaymentsTags.LIST).performScrollToNode(hasTestTag(PaymentsTags.item(id)))
    }

    // Cubre: pay_payment/R1
    @Test
    fun botonPagarSoloEnPendienteYVencido() {
        setScreen(PayUiState())

        listOf("p1", "p2").forEach { id ->
            scrollTo(id)
            compose.onNodeWithTag(
                PaymentsTags.payButton(id)
            ).assertIsDisplayed().assertIsEnabled().assert(shows("Pagar"))
        }
        listOf("p3", "p4").forEach { id ->
            scrollTo(id)
            compose.onNodeWithTag(PaymentsTags.payButton(id)).assertDoesNotExist()
        }
    }

    // Cubre: pay_payment/R1, pay_payment/R2
    @Test
    fun pulsarPagarLlamaOnPayClickConElPago() {
        setScreen(PayUiState())

        scrollTo("p1")
        compose.onNodeWithTag(PaymentsTags.payButton("p1")).performClick()

        assertEquals(listOf(p1), payClicks)
    }

    // Cubre: pay_payment/R2
    @Test
    fun dialogoDeConfirmacionConTextosExactos() {
        setScreen(PayUiState(confirming = p1))

        compose.onNodeWithText("Confirmar pago").assertIsDisplayed()
        compose.onNodeWithText("Vas a pagar $1,250.10 MXN de la cuota que vence el 30 nov 2026.").assertIsDisplayed()
        compose.onNodeWithTag(PaymentsTags.PAY_CONFIRM).assertIsDisplayed().assert(shows("Pagar $1,250.10"))
        compose.onNodeWithTag(PaymentsTags.PAY_CANCEL).assertIsDisplayed().assert(shows("Cancelar"))
    }

    // Cubre: pay_payment/R2, pay_payment/R3
    @Test
    fun confirmarYCancelarLlamanSusCallbacks() {
        setScreen(PayUiState(confirming = p1))

        compose.onNodeWithTag(PaymentsTags.PAY_CONFIRM).performClick()
        assertEquals(1, confirms)

        compose.onNodeWithTag(PaymentsTags.PAY_CANCEL).performClick()
        assertEquals(1, cancels)
    }

    // Cubre: pay_payment/R2
    @Test
    fun sinConfirmacionNoHayDialogo() {
        setScreen(PayUiState())

        compose.onNodeWithText("Confirmar pago").assertDoesNotExist()
        compose.onNodeWithTag(PaymentsTags.PAY_CONFIRM).assertDoesNotExist()
    }

    // Cubre: pay_payment/R3, pay_payment/R4
    @Test
    fun procesandoMuestraBotonDeshabilitado() {
        setScreen(PayUiState(payStates = mapOf("p1" to PayState.Processing)))

        scrollTo("p1")
        compose.onNodeWithTag(PaymentsTags.payButton("p1")).assertIsNotEnabled().assert(shows("Procesando…"))
        compose.onNodeWithTag(PaymentsTags.payButton("p1")).performClick()
        assertEquals(emptyList<Payment>(), payClicks)
    }

    // Cubre: pay_payment/R6, pay_payment/R1
    @Test
    fun verificandoMuestraTextosSinBotonPagar() {
        setScreen(PayUiState(payStates = mapOf("p2" to PayState.Verifying(attemptsLeft = 3))))

        scrollTo("p2")
        compose.onNodeWithTag(PaymentsTags.verifying("p2"))
            .assertIsDisplayed()
            .assert(shows("Verificando tu pago…"))
            .assert(shows("No vuelvas a pagar; te avisaremos en cuanto se confirme."))
        compose.onNodeWithTag(PaymentsTags.payButton("p2")).assertDoesNotExist()
        compose.onNodeWithTag(PaymentsTags.verifyAgain("p2")).assertDoesNotExist()
    }

    // Cubre: pay_payment/R8
    @Test
    fun reintentosAgotadosMuestranVerificarDeNuevo() {
        setScreen(PayUiState(payStates = mapOf("p1" to PayState.Verifying(attemptsLeft = 0))))

        scrollTo("p1")
        compose.onNodeWithTag(PaymentsTags.verifying("p1")).assert(shows("Verificando tu pago…"))
        compose.onNodeWithTag(PaymentsTags.payButton("p1")).assertDoesNotExist()
        compose.onNodeWithTag(PaymentsTags.verifyAgain("p1")).assert(shows("Verificar de nuevo")).performClick()

        assertEquals(listOf("p1"), verifyAgain)
    }

    // Cubre: pay_payment/R5, pay_payment/R1
    @Test
    fun pagoConPayStatePaidSePintaPagadoSinBoton() {
        setScreen(PayUiState(payStates = mapOf("p1" to PayState.Paid)))

        scrollTo("p1")
        compose.onNodeWithTag(PaymentsTags.status("p1")).assert(shows("Pagado"))
        compose.onNodeWithTag(PaymentsTags.payButton("p1")).assertDoesNotExist()
    }

    // Cubre: pay_payment/R13
    @Test
    fun chipDeEstadoNoEsPulsableNiBoton() {
        setScreen(PayUiState())

        listOf("p3", "p2", "p1", "p4").forEach { id ->
            scrollTo(id)
            compose.onNodeWithTag(PaymentsTags.status(id))
                .assertHasNoClickAction()
                .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Role))
        }
    }
}
