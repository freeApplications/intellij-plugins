package jp.freeapps.intellij.plugin.phparray.action

import com.intellij.lang.Language
import com.intellij.psi.PsiFile
import com.jetbrains.php.lang.PhpLanguage
import com.jetbrains.php.lang.psi.PhpFile
import jp.freeapps.intellij.plugin.phparray.converter.PhpArrayConverter

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
        return PhpArrayConverter(psiFile).toJson()
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

    /**
     * Determine if the selected text is a psi file of valid replaceable json text.
     */
    override fun isValid(psiFile: PsiFile): Boolean {
        if (psiFile !is PhpFile) return false
        return PhpArrayConverter(psiFile).isValid()
    }
}