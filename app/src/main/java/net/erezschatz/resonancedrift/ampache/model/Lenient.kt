package net.erezschatz.resonancedrift.ampache.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive

/*
 * Ampache 6 JSON sends ids and counts as strings ("id": "123"), older servers
 * as numbers, and some fields as "" or null when unknown. These two serializers
 * accept all of that, so one odd row never fails a whole list.
 */

/** Ids: a number or a numeric string. Anything else is a real shape error. */
object LenientLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val content = primitiveContent(decoder) ?: return decoder.decodeLong()
        return content.toLongOrNull()
            ?: throw IllegalArgumentException("Expected a numeric id, got '$content'")
    }

    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
}

/** Counts, years, track numbers, durations: unreadable means 0, not a failure. */
object LenientIntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("LenientInt", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        val content = primitiveContent(decoder) ?: return decoder.decodeInt()
        // toDouble covers "240.0"; Ampache has sent floats for durations before.
        return content.toIntOrNull() ?: content.toDoubleOrNull()?.toInt() ?: 0
    }

    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
}

/** The raw text of a JSON primitive (number, string or null), or null when not decoding JSON. */
private fun primitiveContent(decoder: Decoder): String? {
    val jsonDecoder = decoder as? JsonDecoder ?: return null
    val element = jsonDecoder.decodeJsonElement()
    return (element as? JsonPrimitive)?.content.orEmpty()
}
