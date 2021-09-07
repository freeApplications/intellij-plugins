package jp.freeapps.intellij.plugin.phparray.exception

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.refactoring.suggested.endOffset
import com.intellij.refactoring.suggested.startOffset
import java.util.*

class ConvertException(psiElement: PsiElement) : Exception() {
    private val resourceBundle = ResourceBundle.getBundle("messages/PluginBundle")

    var adjustOffset: Int = 0
    val startOffset: Int = psiElement.startOffset
        get() = field + adjustOffset
    val endOffset: Int = psiElement.endOffset
        get() = field + adjustOffset
    override val message: String = if (psiElement is PsiErrorElement) {
        "${resourceBundle.getString("error.syntax")} ( ${psiElement.errorDescription} )"
    } else {
        "${resourceBundle.getString("error.literalType")} ( ${psiElement.text} )"
    }
}