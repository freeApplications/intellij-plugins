package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.json.JsonLanguage
import com.intellij.json.psi.*
import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import jp.freeapps.intellij.plugin.phparray.messages.ErrorMessage
import jp.freeapps.intellij.plugin.phparray.settings.AppSettingsState

class JsonConverter internal constructor(private var psiFile: PsiFile, private var offset: Int) : Converter() {
    // constants
    private val openArray = "array("
    private val closeArray = ")"

    private enum class ManipulateComma {
        NONE, APPEND, REMOVE
    }

    // settings
    private val useBraket: Boolean
    private val useDoubleQuote: Boolean
    private val appendComma: Boolean
    private val quote: String

    // static fields
    companion object {
        val language: Language = JsonLanguage.INSTANCE
        internal const val prefix: String = ""
        internal const val suffix: String = ""
    }

    init {
        val settings = AppSettingsState.getInstance()
        useBraket = settings.useBraket
        useDoubleQuote = settings.useDoubleQuote
        appendComma = settings.appendComma
        quote = if (useDoubleQuote) doubleQuote else singleQuote
        offset += prefix.length
        validateConvertibility()
    }

    /**
     * @inheritdoc
     */
    override fun doConvert(): String {
        if (!hasConversionTarget) return ""
        currentIndex = conversionTarget!!.startOffset
        return toPhpArray(conversionTarget!!)
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
        if (jsonItem == conversionTarget && currentIndex < jsonItem.endOffset) {
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

    /**
     * Validate if it can be conversion.
     */
    private fun validateConvertibility() {
        var jsonItem = psiFile.findElementAt(offset)
        while (jsonItem != null) {
            if (jsonItem is JsonArray || jsonItem is JsonObject) break
            jsonItem = jsonItem.parent
        }
        if (jsonItem == null) return findJsonElement()
        conversionTarget = jsonItem
        conversionTargetText = "${prefix}${jsonItem.text}${suffix}"
        conversionTargetRange = jsonItem.textRange.shiftLeft(prefix.length).shiftRight(adjustOffset ?: 0)
        validateConvertibility(jsonItem)
    }

    private fun validateConvertibility(jsonItem: PsiElement) {
        jsonItem.children.forEach { child ->
            when {
                child is JsonArray || child is JsonObject || child is JsonProperty -> {
                    validateConvertibility(child)
                }
                child is PsiErrorElement -> {
                    errorMessages += ErrorMessage(
                        "error.syntax",
                        child.textRange.shiftLeft(prefix.length).shiftRight(adjustOffset ?: 0),
                        child.errorDescription
                    )
                }
                child !is JsonLiteral && child !is PsiWhiteSpace && child !is PsiComment -> {
                    if (child.prevSibling !is PsiErrorElement) {
                        errorMessages += ErrorMessage(
                            "error.literalType",
                            child.textRange.shiftLeft(prefix.length).shiftRight(adjustOffset ?: 0),
                            child.text
                        )
                    }
                }
            }
        }
    }

    /**
     * Adjust to find JSON
     */
    private var adjustOffset: Int? = null

    private fun findJsonElement() {
        if (adjustOffset != null) return
        var jsonItem = psiFile.findElementAt(offset)
        while (jsonItem != null) {
            if (jsonItem is LeafPsiElement) {
                if (arrayOf(openBrace, openBraket).contains(jsonItem.elementType.toString())) break
            }
            jsonItem = jsonItem.prevSibling ?: jsonItem.parent
        }
        if (jsonItem == null || jsonItem.textOffset == 0) return
        adjustOffset = jsonItem.textOffset
        offset -= adjustOffset!!
        psiFile = createFileFromText(language, psiFile.text.substring(jsonItem.textOffset))
        validateConvertibility()
    }
}