package jp.freeapps.intellij.plugin.phparray.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.util.TextRange
import jp.freeapps.intellij.plugin.phparray.converter.JsonConverter

/**
 * Menu action to replace a selection of characters with a php array string.
 *
 * @see AnAction
 */
class JsonToPhpArrayAction : AnAction() {
    /**
     * Replaces the run of text selected by the primary caret with a php array string.
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
            val jsonConverter = JsonConverter(text)
            val convertedText = jsonConverter.toPhpArray()
            document.replaceString(start, end, convertedText)
            diffLength = convertedText.length - text.length
        }
        // Select the text range that was just replaced
        primaryCaret.setSelection(start, end + diffLength)
    }

    /**
     * Sets visibility and enables this action menu item if:
     *
     *  * a project is open
     *  * an editor is active
     *  * some characters are selected
     *  * selected text is a valid json string
     *
     * @param e Event related to this action
     */
    override fun update(e: AnActionEvent) {
        // Get required data keys
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        // Set visibility and enable only in case of existing project and editor and if a selection is json string
        val hasSelection = project != null && editor != null && editor.selectionModel.hasSelection()
        if (hasSelection) {
            val document = editor!!.document
            val primaryCaret = editor.caretModel.primaryCaret
            val start = primaryCaret.selectionStart
            val end = primaryCaret.selectionEnd
            val text = document.getText(TextRange(start, end))
            val jsonConverter = JsonConverter(text)
            e.presentation.isEnabledAndVisible = jsonConverter.isValid()
        } else {
            e.presentation.isEnabledAndVisible = false
        }
    }
}