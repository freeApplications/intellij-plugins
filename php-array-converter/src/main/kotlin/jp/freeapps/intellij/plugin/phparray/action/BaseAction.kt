package jp.freeapps.intellij.plugin.phparray.action

import com.intellij.codeInsight.hint.HintManager
import com.intellij.lang.Language
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import jp.freeapps.intellij.plugin.phparray.exception.ConvertException

/**
 * Basis for menu actions that replace a selection of characters.
 *
 * @see AnAction
 */
abstract class BaseAction : AnAction() {
    /**
     * Replaces the run of text selected by the primary caret.
     *
     * @param e Event related to this action
     */
    override fun actionPerformed(e: AnActionEvent) {
        // Get all the required data from data keys
        // Editor and Project were verified in update(), so they are not null.
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        val document = editor.document
        // Work off of the primary caret to get the selection info
        val primaryCaret = editor.caretModel.primaryCaret
        val start = primaryCaret.selectionStart
        val end = primaryCaret.selectionEnd
        // Replace the selection with a php array string.
        // Must do this document change in a write action context.
        var diffLength = 0
        WriteCommandAction.runWriteCommandAction(project) {
            val text = document.getText(TextRange(start, end))
            // create psi file
            val factory = PsiFileFactory.getInstance(project)
            val psiFile = factory.createFileFromText(getLanguage(), appendAffixes(text))
            try {
                val replacedText = removeAffixes(replaceSelectedText(psiFile))
                document.replaceString(start, end, replacedText)
                diffLength = replacedText.length - text.length
            } catch (e: ConvertException) {
                HintManager.getInstance().showErrorHint(
                    editor,
                    e.message,
                    start + e.startOffset,
                    start + e.endOffset,
                    HintManager.ABOVE,
                    HintManager.HIDE_BY_ANY_KEY,
                    0
                )
            }
        }
        // Select the text range that was just replaced
        primaryCaret.setSelection(start, end + diffLength)
    }

    /**
     * @param text selected text
     * @return Text with affixes appended to selected text
     */
    protected open fun appendAffixes(text: String): String {
        return text
    }

    /**
     * @param text replaced text
     * @return Text with affixes removed from the replaced text
     */
    protected open fun removeAffixes(text: String): String {
        return text
    }

    /**
     * @return Language of conversion source.
     */
    abstract fun getLanguage(): Language

    /**
     * Replace the selected text with something else useful.
     *
     * @param psiFile psi file of selected text
     * @return replaced text
     */
    abstract fun replaceSelectedText(psiFile: PsiFile): String
}