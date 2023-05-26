@file:Suppress("unused", "UNUSED_PARAMETER")

package net.pacujo.lip

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter

fun JsonWriter.emitObject(content: (JsonWriter) -> Unit): JsonWriter {
    content(beginObject())
    return endObject()
}

fun JsonWriter.emitArray(content: (JsonWriter) -> Unit): JsonWriter {
    content(beginArray())
    return endArray()
}

typealias JsonValue = Any?
typealias JsonParser = JsonReader.(Int) -> JsonValue
typealias JsonObject = Map<String, JsonValue>
typealias JsonArray = List<JsonValue>

class JsonSchema(vararg fields: Pair<String, JsonParser>) {
    private val mapping = fields.toMap()

    operator fun get(name: String) = mapping[name]
}

class JsonLink(val maxDepth: Int? = null) {
    lateinit var parser: JsonParser
}

class JsonSchemaException(message: String? = null, cause: Throwable? = null)
    : Exception(message, cause)

fun JsonReader.expect(vararg tokens: JsonToken) {
    val token = peek()
    if (tokens.contains(token))
        return
    val expected = tokens.joinToString(" or ")
    throw JsonSchemaException("$expected expected (saw $token)")
}

fun JsonReader.expectEnd() = expect(JsonToken.END_DOCUMENT)

fun JsonReader.atEnd() = peek() == JsonToken.END_DOCUMENT

fun JsonReader.atNull() = peek() == JsonToken.NULL

fun JsonReader.skipNull(): Boolean {
    if (atNull()) {
        nextNull()
        return true
    }
    return false
}

fun JsonReader.parseInt(depth: Int = 0): Int {
    expect(JsonToken.NUMBER)
    try {
        return nextInt()
    } catch (e: NumberFormatException) {
        throw(JsonSchemaException(cause = e))
    }
}

fun JsonReader.parseLong(depth: Int = 0): Long {
    expect(JsonToken.NUMBER)
    try {
        return nextLong()
    } catch (e: NumberFormatException) {
        throw(JsonSchemaException(cause = e))
    }
}

fun JsonReader.parseDouble(depth: Int = 0): Double {
    expect(JsonToken.NUMBER)
    return nextDouble()
}

fun JsonReader.parseString(depth: Int = 0): String {
    expect(JsonToken.STRING)
    return nextString()
}

fun JsonReader.parseBoolean(depth: Int = 0): Boolean {
    expect(JsonToken.BOOLEAN)
    return nextBoolean()
}

fun JsonReader.parseArray(
    elementParser: JsonParser,
    depth: Int = 0,
): JsonArray {
    expect(JsonToken.BEGIN_ARRAY)
    beginArray()
    val result = mutableListOf<JsonValue>()
    while (peek() != JsonToken.END_ARRAY)
        result.add(elementParser(depth))
    endArray()
    return result
}

fun JsonReader.parseObject(schema: JsonSchema, depth: Int = 0): JsonObject {
    expect(JsonToken.BEGIN_OBJECT)
    beginObject()
    val result = mutableMapOf<String, JsonValue>()
    while (true) {
        expect(JsonToken.NAME, JsonToken.END_OBJECT)
        if (peek() == JsonToken.END_OBJECT)
            break
        val fieldName = nextName()
        val parser = schema[fieldName]
        if (parser == null)
            skipValue()
        else
            result[fieldName] = parser(depth)
    }
    endObject()
    return result
}

fun JsonReader.parseLink(link: JsonLink, depth: Int = 0): JsonValue {
    if (link.maxDepth != null && depth > link.maxDepth)
        throw JsonSchemaException("Maximum recursion depth exceeded")
    return (link.parser)(depth + 1)
}

object J {
    val int = JsonReader::parseInt
    val long = JsonReader::parseLong
    val double = JsonReader::parseDouble
    val string = JsonReader::parseString
    val boolean = JsonReader::parseBoolean

    fun array(element: JsonParser): JsonParser =
        { parseArray(element) }

    fun obj(schema: JsonSchema): JsonParser =
        { parseObject(schema) }


    fun link(jsonLink: JsonLink): JsonParser =
        { parseLink(jsonLink) }

    fun <T> optional(parser: JsonReader.(Int) -> T): JsonReader.(Int) -> T? {
        return { if (skipNull()) null else parser(it) }

    }
}
