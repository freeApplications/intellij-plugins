package jp.freeapps.intellij.plugin.phparray.toolwindow

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.codeInsight.hint.HintManager
import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.lang.Language
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.Disposable
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.ui.LanguageTextField
import com.intellij.ui.LightweightHint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import jp.freeapps.intellij.plugin.phparray.converter.Converter
import jp.freeapps.intellij.plugin.phparray.messages.ErrorMessage
import jp.freeapps.intellij.plugin.phparray.settings.AppSettingsState
import java.awt.*
import java.awt.GridBagConstraints.*
import java.awt.datatransfer.StringSelection
import java.util.*
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JSplitPane
import javax.swing.ListSelectionModel
import javax.swing.border.LineBorder
import javax.swing.table.AbstractTableModel

class ToolWindowComponent(private val project: Project) : JPanel(GridBagLayout()), Disposable {
    private val resourceBundle = ResourceBundle.getBundle("messages/PluginBundle")
    private var editor: Editor? = null
    private var conversionTarget: EditorTextField? = null

    companion object {
        const val ID = "PHP Array Converter"
        val lineBorder = LineBorder(JBUI.CurrentTheme.ToolWindow.headerBackground())
    }

    fun refreshContent() {
        editor = FileEditorManager.getInstance(project).selectedTextEditor
        if (editor == null) return

        // Clear content
        conversionTarget?.releaseEditor()
        removeAll()

        // Get converter
        val converter = Converter.factory(project, editor!!.caretModel.primaryCaret)

        // Add contents
        createConversionTarget(converter)
        val insets = JBUI.insets(0)
        var constraints = GridBagConstraints(0, 0, 1, 1, 1.0, 1.0, WEST, BOTH, insets, 0, 0)
        if (converter.errorMessages.isEmpty()) {
            border = lineBorder
            add(conversionTarget!!, constraints)
            if (converter.hasConversionTarget) {
                constraints = GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, CENTER, NONE, insets, 0, 0)
                add(createConversionButton(converter), constraints)
            }
        } else {
            border = null
            val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
            splitPane.border = lineBorder
            splitPane.resizeWeight = 0.9
            splitPane.topComponent = conversionTarget
            splitPane.bottomComponent = createErrorMessages(converter)
            add(splitPane, constraints)
        }
    }

    private fun createConversionTarget(converter: Converter) {
        conversionTarget = EditorTextField(
            converter.language ?: PlainTextLanguage.INSTANCE,
            project,
            converter.conversionTargetText
        )
        conversionTarget?.preferredSize = Dimension(width, height)
        conversionTarget?.font = EditorUtil.getEditorFont()
        conversionTarget?.border = null
        conversionTarget?.isViewer = true
        conversionTarget?.isFocusable = false
    }

    private fun createErrorMessages(converter: Converter): JBScrollPane {
        val table = JBTable(MessageTableModel(converter.errorMessages))
        table.tableHeader = null
        table.cellSelectionEnabled = false
        val selectionModel = table.selectionModel
        selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
        selectionModel.addListSelectionListener {
            if (table.selectedRow < 0 || table.selectedColumn < 0) return@addListSelectionListener
            val errorMessage = table.getValueAt(table.selectedRow, table.selectedColumn)
            if (editor != null && errorMessage is ErrorMessage) {
                editor!!.contentComponent.requestFocus()
                val position = editor!!.offsetToLogicalPosition(errorMessage.startOffset)
                editor!!.scrollingModel.scrollTo(position, ScrollType.CENTER)
                editor!!.scrollingModel.runActionOnScrollingFinished {
                    val errorLabel = JBLabel(errorMessage.message)
                    errorLabel.background = JBUI.CurrentTheme.NotificationWarning.backgroundColor()
                    val hint = LightweightHint(errorLabel)
                    HintManagerImpl.getInstanceImpl().showEditorHint(
                        hint,
                        editor!!,
                        HintManagerImpl.getHintPosition(
                            hint,
                            editor!!,
                            editor!!.offsetToLogicalPosition(errorMessage.startOffset),
                            HintManager.ABOVE
                        ),
                        HintManager.HIDE_BY_ANY_KEY,
                        0,
                        false
                    )
                }
            }
            table.clearSelection()
        }
        val scrollPane = JBScrollPane(table)
        scrollPane.border = lineBorder
        return scrollPane
    }

    private fun createConversionButton(converter: Converter): JButton {
        val conversionButton = JButton(resourceBundle.getString(converter.conversionMessageKey))
        conversionButton.font = conversionButton.font.deriveFont(Font.BOLD)
        conversionButton.addActionListener {
            val conversionResult = converter.doConvert()
            if (AppSettingsState.getInstance().replaceInEditor) {
                val primaryCaret = editor!!.caretModel.primaryCaret
                val currentPosition = primaryCaret.logicalPosition
                val document = editor?.document
                val textRange = converter.conversionTargetRange
                WriteCommandAction.runWriteCommandAction(project) {
                    document?.replaceString(textRange!!.startOffset, textRange.endOffset, conversionResult)
                }
                primaryCaret.moveToLogicalPosition(currentPosition)
                refreshContent()
            } else {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                clipboard.setContents(StringSelection(converter.indent + conversionResult), null)
                NotificationGroupManager.getInstance().getNotificationGroup("Conversion Notification")
                    .createNotification(
                        resourceBundle.getString("notification.conversionResult.saveToClipboard"),
                        NotificationType.INFORMATION
                    )
                    .notify(project)
            }
            editor!!.contentComponent.requestFocus()
        }
        return conversionButton
    }

    private class EditorTextField(
        language: Language,
        project: Project,
        text: String
    ) : LanguageTextField(language, project, text) {
        override fun createEditor(): EditorEx {
            val editor = super.createEditor()
            editor.isViewer = true
            editor.isOneLineMode = false
            editor.setBorder(lineBorder)
            editor.setCaretEnabled(false)
            editor.setVerticalScrollbarVisible(true)
            editor.setHorizontalScrollbarVisible(true)
            val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.document)
            if (psiFile != null) {
                DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, false)
            }
            editor.backgroundColor = EditorColorsManager.getInstance().globalScheme.defaultBackground
            return editor
        }

        fun releaseEditor() {
            if (editor != null) {
                EditorFactory.getInstance().releaseEditor(editor!!)
            }
        }
    }

    private class MessageTableModel(private val errorMessages: Array<ErrorMessage>) : AbstractTableModel() {
        private val columnNames = arrayOf("Messages")

        override fun getRowCount(): Int {
            return errorMessages.size
        }

        override fun getColumnCount(): Int {
            return columnNames.size
        }

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            return errorMessages[rowIndex]
        }

        override fun getColumnName(column: Int): String {
            return columnNames[column]
        }
    }

    override fun dispose() {
        conversionTarget?.releaseEditor()
    }
}