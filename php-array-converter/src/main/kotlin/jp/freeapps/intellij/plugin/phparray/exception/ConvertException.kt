package jp.freeapps.intellij.plugin.phparray.exception

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import java.util.*

class ConvertException(
    psiElement: PsiElement,
    messageKey: String,
    showSupplementaryMessage: Boolean = true
) : Exception() {
    private val resourceBundle = ResourceBundle.getBundle("messages/PluginBundle")

    var adjustOffset: Int = 0
    val startOffset: Int = psiElement.startOffset
        get() = field + adjustOffset
    val endOffset: Int = psiElement.endOffset
        get() = field + adjustOffset
    override val message: String = resourceBundle.getString(messageKey)
        get() = field + if (additionalText != null) " ( $additionalText )" else ""
    private val additionalText: String?

    init {
        additionalText = if (showSupplementaryMessage) {
            if (psiElement is PsiErrorElement) psiElement.errorDescription else psiElement.text
        } else null
    }
}