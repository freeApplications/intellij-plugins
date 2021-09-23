package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.psi.*
import com.intellij.psi.impl.source.tree.LeafPsiElement
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
    private val array = "array"
    private val openParentheses = "("
    private val closeParentheses = ")"
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
                if (currentIndex < child.startOffset) {
                    builder.append(phpText.substring(currentIndex, child.startOffset))
                    currentIndex = child.startOffset
                }
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
                syntax = replacePreviousArraySyntax(child.prevSibling, isJsonObject)
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
        val syntaxFirstChild = if (phpItem.children.isNotEmpty()) {
            phpItem.children[phpItem.children.size - 1].nextSibling
        } else phpItem.firstChild
        builder.append(replaceArraySyntax(syntaxFirstChild, phpItem.lastChild, isJsonObject))
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
            builder.append(phpText.substring(currentIndex, phpItem.startOffset))
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
            builder.append(phpText.substring(currentIndex, startOffset))
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
            val item = if (phpItem.prevSibling == null) phpItem.parent.prevSibling else phpItem.prevSibling
            builder.append(replacePreviousArraySyntax(item))
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

    private fun replacePreviousArraySyntax(latestItem: PsiElement, isJsonObject: Boolean = false): String {
        var item = latestItem
        while (true) {
            if (item.prevSibling == null || item.prevSibling.endOffset <= currentIndex) break
            item = item.prevSibling
        }
        return replaceArraySyntax(item, latestItem, isJsonObject)
    }

    private fun replaceArraySyntax(first: PsiElement, last: PsiElement, isJsonObject: Boolean): String {
        val hasArrayComment = fun(array: PsiElement): Boolean {
            var subItem: PsiElement? = array
            while (subItem != null) {
                if (subItem is PsiComment) {
                    return true
                }
                if (subItem == last || (subItem is LeafPsiElement && subItem.elementType.toString() == openParentheses)) break
                subItem = subItem.nextSibling
            }
            return false
        }
        val builder = StringBuilder()
        var skipWhiteSpace = false
        var item: PsiElement? = first
        while (item != null) {
            val text = if (item is PsiComment) item.text else replaceArraySyntax(item.text, isJsonObject)
            if (!skipWhiteSpace || item !is PsiWhiteSpace) builder.append(text)
            if (item == last) break
            if (item is LeafPsiElement && item.elementType.toString() == array) {
                skipWhiteSpace = !hasArrayComment(item)
            } else if (skipWhiteSpace && item is LeafPsiElement && item.elementType.toString() == openParentheses) {
                skipWhiteSpace = false
            }
            item = item.nextSibling
        }
        return builder.toString()
    }

    private fun replaceArraySyntax(string: String, isJsonObject: Boolean): String {
        return string
            .replace(array, "")
            .replace(openParentheses, if (isJsonObject) openBrace else openBraket)
            .replace(openBraket, if (isJsonObject) openBrace else openBraket)
            .replace(closeParentheses, if (isJsonObject) closeBrace else closeBraket)
            .replace(closeBraket, if (isJsonObject) closeBrace else closeBraket)
            .replace(arrow, colon)
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
        phpItem.children.forEach { child ->
            when (child) {
                is Statement, is ArrayCreationExpression -> {
                    checkValidType(child)
                    return@forEach
                }
                else -> {
                    if (phpItem is Statement) {
                        throw ConvertException(phpItem, "error.rootElement.phpArray", false)
                    }
                }
            }
            when {
                child is PsiErrorElement -> {
                    throw ConvertException(child, "error.syntax")
                }
                child is ArrayHashElement || child is UnaryExpression -> {
                    checkValidType(child)
                }
                child is PhpPsiElement && arrayElement.contains(child.elementType.toString()) -> {
                    checkValidType(child)
                }
                child is ConstantReference -> {
                    if (!constantReferenceType.contains(child.type.toString())) {
                        throw ConvertException(child, "error.literalType")
                    }
                }
                child !is StringLiteralExpression && child !is PsiWhiteSpace -> {
                    if (child !is PhpPsiElement || child.elementType.toString() != number) {
                        throw ConvertException(child, "error.literalType")
                    }
                }
            }
        }
    }
}