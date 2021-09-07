package jp.freeapps.intellij.plugin.phparray.action

import com.intellij.lang.Language
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.PhpFile
import jp.freeapps.intellij.plugin.phparray.converter.PhpArrayConverter
import jp.freeapps.intellij.plugin.phparray.exception.ConvertException

/**
 * Menu action to replace a selection of characters with a json string.
 *
 * @see BaseAction
 */
class PhpArrayToJsonAction : BaseAction() {
    private val phpOpeningTag = "<?php\n"
    private val semicolon = ";"

    /**
     * @inheritDoc
     */
    override fun getLanguage(): Language {
        return PhpLanguage.INSTANCE
    }

    /**
     * Replace the selected text with json string.
     */
    override fun replaceSelectedText(psiFile: PsiFile): String {
        if (psiFile !is PhpFile) return psiFile.text
        try {
            return PhpArrayConverter(psiFile).toJson()
        } catch (e: ConvertException) {
            e.adjustOffset = phpOpeningTag.length * -1
            throw e
        }
    }

    /**
     * @inheritDoc
     */
    override fun appendAffixes(text: String): String {
        return "$phpOpeningTag$text$semicolon"
    }

    /**
     * @inheritDoc
     */
    override fun removeAffixes(text: String): String {
        return text.substring(phpOpeningTag.length, text.length - semicolon.length)
    }
}