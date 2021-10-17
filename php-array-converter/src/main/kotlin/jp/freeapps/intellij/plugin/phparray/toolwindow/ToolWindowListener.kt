package jp.freeapps.intellij.plugin.phparray.toolwindow

import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.openapi.wm.ex.ToolWindowManagerListener
import com.intellij.util.messages.MessageBusConnection

class ToolWindowListener(private val project: Project) : ToolWindowManagerListener {
    private var component: ToolWindowComponent? = null
    private var caretListener: CaretListener? = null
    private var connection: MessageBusConnection? = null

    override fun stateChanged(toolWindowManager: ToolWindowManager) {
        val toolWindow = toolWindowManager.getToolWindow(ToolWindowComponent.ID) ?: return
        val component = toolWindow.contentManager.component.components.first()
        if (component !is ToolWindowComponent) {
            return
        }
        val eventMulticaster = EditorFactory.getInstance().eventMulticaster
        if (toolWindow.isVisible) {
            this.component = component
            if (component.components.isEmpty()) {
                component.refreshContent()
            }
            if (caretListener == null) {
                caretListener = object : CaretListener {
                    var position: LogicalPosition? = null
                    override fun caretPositionChanged(event: CaretEvent) {
                        if (!event.editor.isViewer && position != event.newPosition) {
                            position = event.newPosition
                            component.refreshContent()
                        }
                    }
                }
                eventMulticaster.addCaretListener(caretListener!!, project)
            }
            if (connection == null) {
                connection = project.messageBus.connect(component)
                connection!!.subscribe(
                    FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
                        override fun selectionChanged(event: FileEditorManagerEvent) {
                            component.refreshContent()
                        }
                    }
                )
            }
        } else {
            this.component = null
            if (caretListener != null) {
                eventMulticaster.removeCaretListener(caretListener!!)
                caretListener = null
            }
            if (connection != null) {
                connection?.disconnect()
                connection = null
            }
        }
    }
}