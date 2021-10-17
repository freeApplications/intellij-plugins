package jp.freeapps.intellij.plugin.phparray.action

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.wm.ToolWindowManager
import jp.freeapps.intellij.plugin.phparray.toolwindow.ToolWindowComponent

/**
 * Open the PHP Array Converter tool window.
 *
 * @see AnAction
 */
class ShowToolWindowAction : AnAction() {
    /**
     * Open the PHP Array Converter tool window.
     *
     * @param e Event related to this action
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getRequiredData(CommonDataKeys.PROJECT)
        ToolWindowManager.getInstance(project).getToolWindow(ToolWindowComponent.ID)?.show(null)
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