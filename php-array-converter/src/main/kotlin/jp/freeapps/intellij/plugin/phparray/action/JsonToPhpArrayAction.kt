package jp.freeapps.intellij.plugin.phparray.action

import com.intellij.json.JsonLanguage
import com.intellij.json.psi.JsonFile
import com.intellij.lang.Language
import com.intellij.psi.PsiFile
import jp.freeapps.intellij.plugin.phparray.converter.JsonConverter
import jp.freeapps.intellij.plugin.phparray.exception.ConvertException

/**
 * Menu action to replace a selection of characters with a php array string.
 *
 * @see BaseAction
 */
class JsonToPhpArrayAction : BaseAction() {
    /**
     * @inheritDoc
     */
    override fun getLanguage(): Language {
        return JsonLanguage.INSTANCE
    }

    /**
     * Replace the selected text with php array string.
     */
    override fun replaceSelectedText(psiFile: PsiFile): String {
        if (psiFile !is JsonFile || psiFile.text.trim().isEmpty()) {
            throw ConvertException(psiFile, "error.rootElement.json", false)
        }
        return JsonConverter(psiFile).toPhpArray()
    }
}