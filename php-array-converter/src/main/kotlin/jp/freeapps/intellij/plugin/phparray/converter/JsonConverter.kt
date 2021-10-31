package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.json.JsonLanguage
import com.intellij.json.psi.*
import com.intellij.lang.Language
import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.rd.util.remove
import jp.freeapps.intellij.plugin.phparray.messages.ErrorMessage
import jp.freeapps.intellij.plugin.phparray.settings.AppSettingsState

class JsonConverter internal constructor(private val psiFile: PsiFile, private var offset: Int) : Converter() {
    // constants
    private val openArray = "array("
    private val closeArray = ")"

    private enum class ManipulateComma {
        NONE, APPEND, REMOVE
    }

    // settings
    private var settings = AppSettingsState.getInstance()
    private val useBraket get() = settings.useBraket
    private val useDoubleQuote get() = settings.useDoubleQuote
    private val appendComma get() = settings.appendComma
    private val quote get() = if (useDoubleQuote) doubleQuote else singleQuote

    // static fields
    companion object {
        val language: Language = JsonLanguage.INSTANCE
        internal const val prefix: String = ""
        internal const val suffix: String = ""
    }

    init {
        offset += prefix.length
        validateConvertibility()
    }

    /**
     * @inheritdoc
     */
    override fun doConvert(): String {
        if (!hasConversionTarget) return ""
        settings = AppSettingsState.getInstance()
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
        findJson()
        var jsonItem = (adjustPsiFile ?: psiFile).findElementAt(adjustOffset ?: offset)
        while (jsonItem != null) {
            if (jsonItem is JsonArray || jsonItem is JsonObject) break
            jsonItem = jsonItem.parent
        }
        if (jsonItem == null) return
        conversionTarget = jsonItem
        conversionTargetText = "$prefix$indent${jsonItem.text}$suffix"
        conversionTargetRange = jsonItem.textRange.shiftLeft(prefix.length)
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
                        child.textRange.shiftLeft(prefix.length),
                        child.errorDescription
                    )
                }
                child !is JsonLiteral && child !is PsiWhiteSpace && child !is PsiComment -> {
                    if (child is JsonReferenceExpression && (child.prevSibling !is PsiErrorElement && child.nextSibling !is PsiErrorElement)) {
                        errorMessages += ErrorMessage(
                            "error.literalType",
                            child.textRange.shiftLeft(prefix.length),
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
    private var adjustPsiFile: PsiFile? = null

    private fun findJson() {
        if (adjustOffset == prefix.length) return
        var offset = this.offset
        var closeArray = arrayOf<String>()
        while (offset >= prefix.length) {
            val jsonItem = psiFile.findElementAt(offset)
            if (jsonItem is LeafPsiElement) {
                when (jsonItem.elementType.toString()) {
                    closeBrace -> {
                        if (offset == this.offset
                            && jsonItem.parent is JsonObject
                            && jsonItem.endOffset == jsonItem.parent.endOffset
                        ) break
                        if (offset != this.offset) closeArray += closeBrace
                    }
                    closeBraket -> {
                        if (offset == this.offset
                            && jsonItem.parent is JsonArray
                            && jsonItem.endOffset == jsonItem.parent.endOffset
                        ) break
                        closeArray += closeBraket
                    }
                    openBrace -> {
                        if (closeArray.contains(closeBrace)) {
                            closeArray = closeArray.remove(closeBrace)
                        } else break
                    }
                    openBraket -> {
                        if (closeArray.contains(closeBraket)) {
                            closeArray = closeArray.remove(closeBraket)
                        } else break
                    }
                }
            }
            offset = if (jsonItem != null) jsonItem.startOffset - 1 else offset - 1
        }
        if (offset < prefix.length) return
        adjustOffset = offset
        val jsonItem = psiFile.findElementAt(offset)
        if (jsonItem != null && (jsonItem.parent is JsonObject || jsonItem.parent is JsonArray)) return
        // parentがJSONじゃない場合、offset以前を任意の文字+インデントで埋めたadjustPsiFileを作成する
        // ※補足：前半にエラーがあるとPSIがJSONと判断してくれない場合があるので、前半部分を亡き者にしつつoffsetは整合性を保たせる
        val beforeOffset = psiFile.text.substring(0, offset)
        val lastLine = beforeOffset.substring(beforeOffset.lastIndexOf("\n") + 1)
        val match = Regex("""^([\t ]*).*""").find(lastLine)
        val indent = (if (match != null && match.groups.size > 1) match.groups[1]?.value else null) ?: ""
        adjustPsiFile = createFileFromText(
            JsonConverter.language,
            prefix + "\n".repeat(offset - indent.length - prefix.length) + indent + psiFile.text.substring(offset)
        )
    }
}