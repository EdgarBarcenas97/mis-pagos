package dev.base.app.feature.payments.data

import java.math.BigDecimal
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonEncoder
import kotlinx.serialization.json.JsonUnquotedLiteral
import kotlinx.serialization.json.jsonPrimitive

/** Lee el número JSON como texto y lo convierte en [BigDecimal] sin pasar por `Double`. */
object BigDecimalSerializer : KSerializer<BigDecimal> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("java.math.BigDecimal", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): BigDecimal {
        // En JSON, el contenido del primitivo es el texto tal cual llegó ("1250.10"): se conserva la escala.
        val text = if (decoder is JsonDecoder) {
            decoder.decodeJsonElement().jsonPrimitive.content
        } else {
            decoder.decodeString()
        }
        return BigDecimal(text)
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun serialize(encoder: Encoder, value: BigDecimal) {
        if (encoder is JsonEncoder) {
            encoder.encodeJsonElement(JsonUnquotedLiteral(value.toPlainString()))
        } else {
            encoder.encodeString(value.toPlainString())
        }
    }
}
