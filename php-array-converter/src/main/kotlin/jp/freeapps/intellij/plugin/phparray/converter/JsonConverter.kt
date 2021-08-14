package jp.freeapps.intellij.plugin.phparray.converter

import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

class JsonConverter(json: String) {
    // constants
    private val openBraket = "["
    private val closeBraket = "]"
    private val openBrace = "{"
    private val closeBrace = "}"
    private val openArray = "array("
    private val closeArray = ")"
    private val colon = ":"
    private val arrow = " => "
    private val singleQuote = "'"
    private val doubleQuote = "\""

    // variables
    private var jsonString: String = json
    private var jsonObject: Any? = null
    private var currentIndex: Int = 0
    private var useBraket = false
    private var useDoubleQuote = false

    init {
        val mapper = ObjectMapper()
        try {
            jsonObject = mapper.readValue<Any>(json, object : TypeReference<Any?>() {})
        } catch (jsonProcessingException: JsonProcessingException) {
            // invalid json string.
        }
    }

    fun isValid(): Boolean {
        return jsonObject != null && (jsonObject is List<*> || jsonObject is Map<*, *>)
    }

    fun setUseBraket(useBraket: Boolean): JsonConverter {
        this.useBraket = useBraket
        return this
    }

    fun setUseDoubleQuote(useDoubleQuote: Boolean): JsonConverter {
        this.useDoubleQuote = useDoubleQuote
        return this
    }

    fun toPhpArray(): String {
        if (!isValid()) {
            return ""
        }
        currentIndex = 0
        return toPhpArray(jsonObject as Any)
    }

    private fun toPhpArray(jsonItem: Any, hierarchy: Int = 0): String {
        if (jsonItem is List<*>) {
            return listToPhpArray(jsonItem, hierarchy)
        }
        if (jsonItem is Map<*, *>) {
            return mapToPhpArray(jsonItem, hierarchy)
        }
        // item is a string key or primitive value
        val string: String = jsonItem as? String ?: jsonItem.toString()
        var searchString = replaceEscapeSequence(string, true)
        if (searchString.isEmpty() || isOnlySyntaxCharacters(searchString)) {
            searchString = "$doubleQuote$jsonItem$doubleQuote"
        }
        val positionIndex = jsonString.indexOf(searchString, currentIndex)
        val syntax: String = replaceSyntax(positionIndex)
        currentIndex = positionIndex + searchString.length
        return syntax + if (jsonItem is String) {
            val quote = if (useDoubleQuote) doubleQuote else singleQuote
            "$quote${replaceEscapeSequence(string, useDoubleQuote)}$quote"
        } else jsonItem.toString()
    }

    private fun listToPhpArray(list: List<*>, hierarchy: Int): String {
        val builder = StringBuilder()
        list.forEach { item ->
            builder.append(this.toPhpArray(item as Any, hierarchy + 1))
        }
        if (hierarchy == 0) {
            builder.append(replaceSyntax(jsonString.length))
        }
        return builder.toString()
    }

    private fun mapToPhpArray(map: Map<*, *>, hierarchy: Int): String {
        val builder = StringBuilder()
        map.forEach { item ->
            builder
                .append(this.toPhpArray(item.key as Any))
                .append(this.toPhpArray(item.value as Any, hierarchy + 1))
        }
        if (hierarchy == 0) {
            builder.append(replaceSyntax(jsonString.length))
        }
        return builder.toString()
    }

    private fun replaceEscapeSequence(string: String, useDoubleQuote: Boolean): String {
        return string
            .replace("\\", "\\\\")
            .replace("\b", "\\b")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace(
                if (useDoubleQuote) doubleQuote else singleQuote,
                if (useDoubleQuote) "\\$doubleQuote" else "\\$singleQuote"
            )
    }

    private fun isOnlySyntaxCharacters(string: String): Boolean {
        return Regex("""^[{\[":, \]}]+$""").matches(string)
    }

    private fun replaceSyntax(positionIndex: Int): String {
        return jsonString.substring(currentIndex, positionIndex)
            .replace(openBrace, openBraket)
            .replace(closeBrace, closeBraket)
            .replace(openBraket, if (useBraket) openBraket else openArray)
            .replace(closeBraket, if (useBraket) closeBraket else closeArray)
            .replace(" $colon ", arrow)
            .replace(" $colon", arrow)
            .replace("$colon ", arrow)
            .replace(colon, arrow)
            .replace(doubleQuote, "")
    }
}