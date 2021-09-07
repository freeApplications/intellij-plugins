package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import com.jetbrains.php.lang.psi.PhpFile
import com.jetbrains.php.lang.psi.elements.*
import jp.freeapps.intellij.plugin.phparray.exception.ConvertException

class PhpArrayConverter(psiFile: PhpFile) {
    // constants
    private val openBraket = "["
    private val closeBraket = "]"
    private val openBrace = "{"
    private val closeBrace = "}"
    private val openArray = Regex("""array\s*\(""")
    private val closeArray = ")"
    private val colon = ":"
    private val arrow = "=>"
    private val singleQuote = "'"
    private val doubleQuote = "\""
    private val comma = ","
    private val arrayKey = "Array key"
    private val arrayValue = "Array value"
    private val arrayElement = arrayOf(arrayKey, arrayValue)
    private val number = "Number"
    private val integer = "integer"
    private val constantReferenceType = arrayOf("bool", "false", "null")

    // variables
    private val phpFile = psiFile
    private val phpText = psiFile.text
    private var currentIndex = 0

    fun toJson(): String {
        checkValidType(phpFile)
        currentIndex = 0
        return toJson(phpFile)
    }

    private fun toJson(phpItem: PsiElement): String {
        val builder = StringBuilder(phpItem.textLength)
        phpItem.children.forEach { child ->
            if (child is ArrayCreationExpression) {
                builder.append(toJson(child))
            } else {
                builder.append(toJson(child))
            }
        }
        if (phpItem is PsiFile && currentIndex < phpItem.endOffset) {
            builder.append(phpText.substring(currentIndex))
        }
        return builder.toString()
    }

    private fun toJson(phpItem: ArrayCreationExpression): String {
        val isJsonObject = hasArrayHashElement(phpItem)
        val builder = StringBuilder(phpItem.textLength)
        var index = 0
        phpItem.children.forEach { child ->
            var syntax = phpText.substring(currentIndex, child.startOffset)
            if (child.equals(phpItem.firstPsiChild)) {
                syntax = replaceArraySyntax(syntax, isJsonObject)
            }
            builder.append(syntax)
            currentIndex = child.startOffset

            when (child) {
                is ArrayHashElement -> {
                    builder.append(toJsonKey(child.key as PhpPsiElement))
                    val intValue = getIntValue(child.key as PhpPsiElement)
                    if (intValue != null && intValue >= index) {
                        index = intValue + 1
                    }
                    builder.append(toJsonValue(child.value as PhpPsiElement))
                }
                is PhpPsiElement -> {
                    if (isJsonObject) {
                        builder.append(toJsonKey(index, child.startOffset))
                        index++
                    }
                    builder.append(toJsonValue(child))
                }
            }
        }
        val syntax = phpText.substring(currentIndex, phpItem.endOffset)
        builder.append(replaceArraySyntax(syntax, isJsonObject))
        currentIndex = phpItem.endOffset
        return builder.toString()
    }

    private fun hasArrayHashElement(phpItem: ArrayCreationExpression): Boolean {
        phpItem.children.forEach { child ->
            if (child is ArrayHashElement) return true
        }
        return false
    }

    private fun getIntValue(phpItem: PhpPsiElement): Int? {
        val firstChild = phpItem.firstPsiChild
        if (firstChild is StringLiteralExpression || firstChild is ConstantReference || firstChild is ArrayCreationExpression) {
            return null
        }
        val elementType = firstChild?.firstChild?.elementType?.toString()
        if (elementType == integer) {
            return firstChild.text.toInt()
        }
        return null
    }

    private fun toJsonKey(phpItem: PhpPsiElement): String {
        val builder = StringBuilder()
        if (currentIndex < phpItem.startOffset) {
            val syntax = phpText.substring(currentIndex, phpItem.startOffset)
            builder.append(syntax)
        }
        when (phpItem) {
            is StringLiteralExpression -> {
                val value = phpItem.text.substring(1, phpItem.textLength - 1)
                builder.append("$doubleQuote${replaceEscapeQuote(value, phpItem.isSingleQuote)}$doubleQuote")
            }
            else -> builder.append("$doubleQuote${phpItem.text}$doubleQuote")
        }
        currentIndex = phpItem.endOffset
        return builder.toString()
    }

    private fun toJsonKey(index: Int, startOffset: Int): String {
        val builder = StringBuilder()
        if (currentIndex < startOffset) {
            val syntax = phpText.substring(currentIndex, startOffset)
            builder.append(syntax)
        }
        builder.append("$doubleQuote$index$doubleQuote$colon")
        currentIndex = startOffset
        return builder.toString()
    }

    private fun toJsonValue(phpItem: PhpPsiElement): String {
        if (phpItem.elementType.toString() == arrayValue) {
            return toJsonValue(phpItem.firstPsiChild as PhpPsiElement)
        }
        val builder = StringBuilder()
        if (currentIndex < phpItem.startOffset) {
            val syntax = phpText.substring(currentIndex, phpItem.startOffset)
            builder.append(syntax.replace(arrow, colon))
            currentIndex = phpItem.startOffset
        }
        when (phpItem) {
            is ArrayCreationExpression -> {
                builder.append(toJson(phpItem))
            }
            is StringLiteralExpression -> {
                val value = phpItem.text.substring(1, phpItem.textLength - 1)
                builder.append("$doubleQuote${replaceEscapeQuote(value, phpItem.isSingleQuote)}$doubleQuote")
            }
            else -> builder.append(phpItem.text)
        }
        currentIndex = phpItem.endOffset
        return builder.toString()
    }

    private fun replaceArraySyntax(string: String, isJsonObject: Boolean): String {
        return string
            .replace(openArray, if (isJsonObject) openBrace else openBraket)
            .replace(openBraket, if (isJsonObject) openBrace else openBraket)
            .replace(closeArray, if (isJsonObject) closeBrace else closeBraket)
            .replace(closeBraket, if (isJsonObject) closeBrace else closeBraket)
            .replace(comma, "")
    }

    private fun replaceEscapeQuote(string: String, isSingleQuote: Boolean): String {
        if (isSingleQuote) {
            return string
                .replace("\\$singleQuote", singleQuote)
                .replace(doubleQuote, "\\$doubleQuote")
        }
        return string
    }

    private fun checkValidType(phpItem: PsiElement) {
        val errors = PsiTreeUtil.getChildrenOfType(phpItem, PsiErrorElement::class.java)
        if (errors != null && errors.isNotEmpty()) throw ConvertException(errors.first())
        phpItem.children.forEach { child ->
            when {
                child is Statement || child is ArrayCreationExpression || child is ArrayHashElement || child is UnaryExpression -> {
                    checkValidType(child)
                }
                child is PhpPsiElement && arrayElement.contains(child.elementType.toString()) -> {
                    checkValidType(child)
                }
                child is ConstantReference -> {
                    if (!constantReferenceType.contains(child.type.toString())) throw ConvertException(child)
                }
                child !is StringLiteralExpression && child !is PsiWhiteSpace -> {
                    if (child !is PhpPsiElement || child.elementType.toString() != number) throw ConvertException(child)
                }
            }
        }
    }
}