package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.lang.Language
import com.intellij.openapi.editor.Caret
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import jp.freeapps.intellij.plugin.phparray.messages.ErrorMessage

abstract class Converter internal constructor() {
    // constants
    protected val openBraket = "["
    protected val closeBraket = "]"
    protected val openBrace = "{"
    protected val closeBrace = "}"
    protected val openParentheses = "("
    protected val closeParentheses = ")"
    protected val singleQuote = "'"
    protected val doubleQuote = "\""
    protected val colon = ":"
    protected val arrow = "=>"
    protected val comma = ","

    // variables
    protected var currentIndex = 0

    // judgment result whether conversion is possible
    protected var conversionTarget: PsiElement? = null
    var conversionTargetText: String = ""
        protected set
    val hasConversionTarget
        get() = conversionTarget != null
    var conversionTargetRange: TextRange? = null
        protected set
    var errorMessages = arrayOf<ErrorMessage>()
        protected set

    /**
     * Execute convert
     *
     * @return converted result
     */
    abstract fun doConvert(): String

    companion object {
        private var project: Project? = null
        fun factory(project: Project, caret: Caret): Converter = createConverter(project, caret)
        private fun createConverter(project: Project, caret: Caret): Converter {
            this.project = project
            val text = caret.editor.document.text
            val offset = caret.offset

            // Create PhpArrayConverter
            val phpFile = createFileFromText(
                PhpArrayConverter.language,
                "${PhpArrayConverter.prefix}${text}${PhpArrayConverter.suffix}"
            )
            val phpArrayConverter = PhpArrayConverter(phpFile, offset)
            if (phpArrayConverter.hasConversionTarget && phpArrayConverter.errorMessages.isEmpty()) {
                return phpArrayConverter
            }

            // Create JsonConverter
            val jsonFile = createFileFromText(
                JsonConverter.language,
                "${JsonConverter.prefix}${text}${JsonConverter.suffix}"
            )
            val jsonConverter = JsonConverter(jsonFile, offset)
            if (jsonConverter.hasConversionTarget && jsonConverter.errorMessages.isEmpty()) {
                return jsonConverter
            }

            // Return what cannot be conversion.
            if (phpArrayConverter.errorMessages.isNotEmpty()) return phpArrayConverter
            if (jsonConverter.errorMessages.isNotEmpty()) return jsonConverter
            return object : Converter() {
                override fun doConvert(): String {
                    return ""
                }

                init {
                    errorMessages = arrayOf(
                        ErrorMessage(
                            "error.notFound.conversionTarget",
                            TextRange(offset, offset)
                        )
                    )
                }
            }
        }

        internal fun createFileFromText(language: Language, text: String): PsiFile {
            val psiFileFactory = PsiFileFactory.getInstance(project)
            return psiFileFactory.createFileFromText(language, text)
        }
    }
}