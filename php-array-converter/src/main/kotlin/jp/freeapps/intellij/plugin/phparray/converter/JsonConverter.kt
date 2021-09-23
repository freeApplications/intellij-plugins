package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.json.psi.*
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
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

    private enum class ManipulateComma {
        NONE, APPEND, REMOVE
    }

    // variables
    private val jsonFile = psiFile
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
        if (currentIndex < jsonItem.startOffset) {
            builder.append(replacePreviousSyntax(jsonItem.prevSibling))
            currentIndex = jsonItem.prevSibling.endOffset
        }
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
        if (jsonItem is JsonArray || jsonItem is JsonObject) {
            if (jsonItem.children.isEmpty()) {
                builder.append(replaceSyntax(jsonItem.firstChild, jsonItem.lastChild))
            } else {
                val syntaxFirstChild = jsonItem.children[jsonItem.children.size - 1].nextSibling
                if (appendComma) {
                    builder.append(replaceSyntax(syntaxFirstChild, jsonItem.lastChild, ManipulateComma.APPEND))
                } else {
                    builder.append(replaceSyntax(syntaxFirstChild, jsonItem.lastChild, ManipulateComma.REMOVE))
                }
            }
            currentIndex = jsonItem.endOffset
        }
        if (jsonItem is PsiFile && currentIndex < jsonItem.endOffset) {
            builder.append(replacePreviousSyntax(jsonItem.lastChild))
        }
        return builder.toString()
    }

    private fun toPhpArray(jsonItem: JsonLiteral): String {
        val builder = StringBuilder()
        if (currentIndex < jsonItem.startOffset) {
            builder.append(replacePreviousSyntax(jsonItem.prevSibling))
            currentIndex = jsonItem.prevSibling.endOffset
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

    private fun replacePreviousSyntax(latestItem: PsiElement): String {
        var item = latestItem
        while (true) {
            if (item.prevSibling == null || item.prevSibling.endOffset <= currentIndex) break
            item = item.prevSibling
        }
        return replaceSyntax(item, latestItem)
    }

    private fun replaceSyntax(
        first: PsiElement,
        last: PsiElement,
        manipulateComma: ManipulateComma = ManipulateComma.NONE
    ): String {
        val builder = StringBuilder()
        var hasComma = false
        var item: PsiElement? = first
        while (item != null) {
            val isComma = item is LeafPsiElement && item.elementType.toString() == comma
            if (manipulateComma == ManipulateComma.NONE || manipulateComma == ManipulateComma.APPEND || !isComma) {
                builder.append(if (item is PsiComment) item.text else replaceSyntax(item.text))
            }
            hasComma = hasComma || isComma
            if (item == last) break
            item = item.nextSibling
        }
        if (manipulateComma == ManipulateComma.APPEND && !hasComma) {
            builder.insert(0, comma)
        }
        return builder.toString()
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
        jsonItem.children.forEach { child ->
            when {
                child is JsonArray || child is JsonObject -> {
                    checkValidType(child)
                    return@forEach
                }
                child !is PsiWhiteSpace && child !is PsiComment -> {
                    if (jsonItem is JsonFile) {
                        throw ConvertException(jsonItem, "error.rootElement.json", false)
                    }
                }
            }
            when {
                child is PsiErrorElement -> {
                    throw ConvertException(child, "error.syntax")
                }
                child is JsonProperty -> {
                    checkValidType(child)
                }
                child !is JsonLiteral && child !is PsiWhiteSpace && child !is PsiComment -> {
                    throw ConvertException(child, "error.literalType")
                }
            }
        }
    }
}