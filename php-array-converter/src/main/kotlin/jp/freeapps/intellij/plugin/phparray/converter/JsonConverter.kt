package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.json.psi.*
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import jp.freeapps.intellij.plugin.phparray.exception.ConvertException
import jp.freeapps.intellij.plugin.phparray.settings.AppSettingsState

class JsonConverter(psiFile: JsonFile) {
    // constants
    private val openBraket = "["
    private val closeBraket = "]"
    private val openBrace = "{"
    private val closeBrace = "}"
    private val openArray = "array("
    private val closeArray = ")"
    private val colon = ":"
    private val arrow = "=>"
    private val singleQuote = "'"
    private val doubleQuote = "\""
    private val comma = ","

    // variables
    private val jsonFile = psiFile
    private val jsonText = psiFile.text
    private var currentIndex = 0

    // settings
    private val useBraket: Boolean
    private val useDoubleQuote: Boolean
    private val appendComma: Boolean
    private val quote: String

    init {
        val settings = AppSettingsState.getInstance()
        useBraket = settings.useBraket
        useDoubleQuote = settings.useDoubleQuote
        appendComma = settings.appendComma
        quote = if (useDoubleQuote) doubleQuote else singleQuote
    }

    fun toPhpArray(): String {
        checkValidType(jsonFile)
        currentIndex = 0
        return toPhpArray(jsonFile)
    }

    private fun toPhpArray(jsonItem: PsiElement): String {
        val builder = StringBuilder(jsonItem.textLength)
        jsonItem.children.forEach { child ->
            when (child) {
                is JsonArray, is JsonObject -> {
                    builder.append(toPhpArray(child))
                }
                is JsonProperty -> {
                    builder.append(toPhpArray(child))
                }
                is JsonLiteral -> {
                    builder.append(toPhpArray(child))
                }
            }
        }
        if (appendComma && builder.isNotEmpty() && (jsonItem is JsonArray || jsonItem is JsonObject)) {
            builder.append(comma)
            val syntax = jsonText.substring(currentIndex, jsonItem.endOffset)
            builder.append(replaceSyntax(syntax))
            currentIndex = jsonItem.endOffset
        }
        if (jsonItem is PsiFile && currentIndex < jsonItem.endOffset) {
            val syntax = jsonText.substring(currentIndex)
            builder.append(replaceSyntax(syntax))
        }
        return builder.toString()
    }

    private fun toPhpArray(jsonItem: JsonLiteral): String {
        val builder = StringBuilder()
        if (currentIndex < jsonItem.startOffset) {
            val syntax = jsonText.substring(currentIndex, jsonItem.startOffset)
            builder.append(replaceSyntax(syntax))
        }
        var value = jsonItem.text
        if (jsonItem is JsonStringLiteral) {
            value = replaceEscapeQuote(value.substring(1, jsonItem.textLength - 1))
            value = "$quote$value$quote"
        }
        builder.append(value)
        currentIndex = jsonItem.endOffset
        return builder.toString()
    }

    private fun replaceEscapeQuote(string: String): String {
        if (useDoubleQuote) {
            return string
        }
        return string
            .replace(singleQuote, "\\$singleQuote")
            .replace("\\$doubleQuote", doubleQuote)
    }

    private fun replaceSyntax(syntax: String): String {
        return syntax
            .replace(openBrace, openBraket)
            .replace(closeBrace, closeBraket)
            .replace(openBraket, if (useBraket) openBraket else openArray)
            .replace(closeBraket, if (useBraket) closeBraket else closeArray)
            .replace(colon, arrow)
    }

    private fun checkValidType(jsonItem: PsiElement) {
        val errors = PsiTreeUtil.getChildrenOfType(jsonItem, PsiErrorElement::class.java)
        if (errors != null && errors.isNotEmpty()) throw ConvertException(errors.first())
        jsonItem.children.forEach { child ->
            when {
                child is JsonArray || child is JsonObject || child is JsonProperty -> {
                    checkValidType(child)
                }
                child !is JsonLiteral && child !is PsiWhiteSpace -> {
                    throw ConvertException(child)
                }
            }
        }
    }
}