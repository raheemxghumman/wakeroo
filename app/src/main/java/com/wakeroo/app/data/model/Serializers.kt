package com.wakeroo.app.data.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import java.time.DayOfWeek
import java.time.LocalDate

val WakerooJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

object LocalDateSerializer : KSerializer<LocalDate?> {
    override val descriptor = PrimitiveSerialDescriptor("LocalDate", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: LocalDate?) = encoder.encodeString(value?.toString() ?: "")
    override fun deserialize(decoder: Decoder): LocalDate? = decoder.decodeString().takeIf { it.isNotEmpty() }?.let(LocalDate::parse)
}

/** Days are stored as a 7-bit mask, Monday = bit 0. */
object DaySetSerializer : KSerializer<Set<DayOfWeek>> {
    override val descriptor = PrimitiveSerialDescriptor("DaySet", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Set<DayOfWeek>) = encoder.encodeInt(value.toMask())
    override fun deserialize(decoder: Decoder): Set<DayOfWeek> = decoder.decodeInt().toDaySet()
}

fun Set<DayOfWeek>.toMask(): Int = fold(0) { acc, d -> acc or (1 shl (d.value - 1)) }

fun Int.toDaySet(): Set<DayOfWeek> = DayOfWeek.entries.filterTo(LinkedHashSet()) { this and (1 shl (it.value - 1)) != 0 }
