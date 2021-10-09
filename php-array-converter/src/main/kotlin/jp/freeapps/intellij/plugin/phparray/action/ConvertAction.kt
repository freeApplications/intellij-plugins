package jp.freeapps.intellij.plugin.phparray.action

import com.intellij.codeInsight.hint.HintManager
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import jp.freeapps.intellij.plugin.phparray.converter.Converter
import jp.freeapps.intellij.plugin.phparray.settings.AppSettingsState
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.util.*

/**
 * If around the primary caret is conversion target, replace it.
 *
 * @see AnAction
 */
class ConvertAction : AnAction() {
    private val resourceBundle = ResourceBundle.getBundle("messages/PluginBundle")

    /**
     * Replacing around the primary caret position.
     *
     * @param e Event related to this action
     */
    override fun actionPerformed(e: AnActionEvent) {
        // Get all the required data from data keys
        // Editor and Project were verified in update(), so they are not null.
        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val project = e.getRequiredData(CommonDataKeys.PROJECT)

        // Get current position
        val currentPosition = editor.caretModel.primaryCaret.logicalPosition

        // Get converter
        val converter = Converter.factory(project, editor.caretModel.primaryCaret)

        // If not empty error messages, exit.
        if (converter.errorMessages.isNotEmpty()) {
            val errorMessage = converter.errorMessages.first()
            HintManager.getInstance().showErrorHint(
                editor,
                errorMessage.message,
                errorMessage.startOffset,
                errorMessage.endOffset,
                HintManager.ABOVE,
                HintManager.HIDE_BY_ANY_KEY,
                0
            )
            return
        }

        // Run convert
        val conversionResult = converter.doConvert()
        if (AppSettingsState.getInstance().replaceInEditor) {
            val document = editor.document
            val textRange = converter.conversionTargetRange
            WriteCommandAction.runWriteCommandAction(project) {
                document.replaceString(textRange!!.startOffset, textRange.endOffset, conversionResult)
            }
        } else {
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            clipboard.setContents(StringSelection(conversionResult), null)
            NotificationGroupManager.getInstance().getNotificationGroup("Conversion Notification")
                .createNotification(
                    resourceBundle.getString("notification.conversionResult.saveToClipboard"),
                    NotificationType.INFORMATION
                )
                .notify(project)
        }

        // Undo the caret position.
        editor.caretModel.primaryCaret.moveToLogicalPosition(currentPosition)
    }

    /**
     * Sets visibility and enables this action menu item if:
     *
     *  * a project is open
     *  * an editor is active
     *
     * @param e Event related to this action
     */
    override fun update(e: AnActionEvent) {
        // Get required data keys
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        // Sets visibility and enables
        e.presentation.isEnabledAndVisible = project != null && editor != null
    }
}