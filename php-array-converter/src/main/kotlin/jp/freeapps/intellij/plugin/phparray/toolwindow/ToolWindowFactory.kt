package jp.freeapps.intellij.plugin.phparray.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class ToolWindowFactory : ToolWindowFactory {
    /**
     * Create the tool window content.
     *
     * @param project    current project
     * @param toolWindow current tool window
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val component = ToolWindowComponent(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(component, "", false)
        content.setDisposer(component)
        toolWindow.contentManager.addContent(content)
    }
}