package jp.freeapps.intellij.plugin.phparray.settings

import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBRadioButton
import com.intellij.util.ui.JBUI
import java.awt.GridBagConstraints
import java.awt.GridBagConstraints.*
import java.awt.GridBagLayout
import java.util.*
import javax.swing.ButtonGroup
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JSeparator

/**
 * Supports creating and managing a [JPanel] for the Settings Dialog.
 */
class AppSettingsComponent : JPanel(GridBagLayout()) {
    private val resourceBundle = ResourceBundle.getBundle("messages/PluginBundle")

    // constants
    private val leftInset = 20
    private val rightInset = 10
    private val bottomInset = 4

    // variables
    private var lineCount = 0

    // PHP Array syntax
    @Suppress("DialogTitleCapitalization")
    private val useArrayOption = JBRadioButton(message("array"))
    private val useBraketOption = JBRadioButton(message("braket"))
    private val arraySyntax: Array<JBRadioButton> = arrayOf(useArrayOption, useBraketOption)

    // String quotation marks
    private val useSingleQuoteOption = JBRadioButton(message("singleQuotes"))
    private val useDoubleQuoteOption = JBRadioButton(message("doubleQuotes"))
    private val stringQuotationMarks: Array<JBRadioButton> = arrayOf(useSingleQuoteOption, useDoubleQuoteOption)

    // Add comma to last element
    private val appendCommaOption = JBCheckBox(message("addCommaToLastElement"))

    val preferredFocusedComponent: JComponent
        get() = useArrayOption

    var useBraket: Boolean
        get() = useBraketOption.isSelected
        set(newStatus) {
            useBraketOption.isSelected = newStatus
            useArrayOption.isSelected = !newStatus
        }

    var useDoubleQuote: Boolean
        get() = useDoubleQuoteOption.isSelected
        set(newStatus) {
            useDoubleQuoteOption.isSelected = newStatus
            useSingleQuoteOption.isSelected = !newStatus
        }

    var appendComma: Boolean
        get() = appendCommaOption.isSelected
        set(newStatus) {
            appendCommaOption.isSelected = newStatus
        }

    init {
        // JSON to PHP Array Settings
        addTitle(message("title"))
        addOptions(
            mapOf(
                "${message("phpArraySyntax")} : " to arraySyntax,
                "${message("stringQuotationMarks")} : " to stringQuotationMarks,
                "${message("addCommaToLastElement")} : " to arrayOf(appendCommaOption),
            )
        )
        groupingRadioButtons(arraySyntax)
        groupingRadioButtons(stringQuotationMarks)

        // fill vertically
        val insets = JBUI.insets(0, 0, 0, 0)
        val constraints = GridBagConstraints(0, lineCount, 1, 1, 1.0, 1.0, WEST, BOTH, insets, 0, 0)
        this.add(JPanel(), constraints)
    }

    private fun addTitle(title: String) {
        val titlePanel = JPanel(GridBagLayout())

        var insets = JBUI.insets(0, 0, 0, rightInset)
        var constraints = GridBagConstraints(0, 0, 1, 1, 0.0, 0.0, WEST, NONE, insets, 0, 0)
        titlePanel.add(JBLabel(title), constraints)

        insets = JBUI.insets(0, 0, 0, 0)
        constraints = GridBagConstraints(1, 0, 1, 1, 1.0, 0.0, WEST, HORIZONTAL, insets, 0, 0)
        titlePanel.add(JSeparator(JSeparator.HORIZONTAL), constraints)

        insets = JBUI.insets(0, 0, bottomInset, 0)
        constraints = GridBagConstraints(0, lineCount++, 1, 1, 1.0, 0.0, WEST, HORIZONTAL, insets, 0, 0)
        this.add(titlePanel, constraints)
    }

    private fun addOptions(options: Map<String, Array<out JComponent>>) {
        fun showLabel(items: Array<out JComponent>): Boolean {
            return items.size > 1 || items[0] !is JBCheckBox
        }

        val optionsPanel = JPanel(GridBagLayout())
        val maxColumns = options.values.fold(0) { accumulator, items ->
            (if (items.size > accumulator) items.size else accumulator) + (if (showLabel(items)) 1 else 0)
        }

        var rowCount = 0
        options.forEach { (label, items) ->
            var columnCount = 0
            var insets = JBUI.insets(0, 0, bottomInset, rightInset)
            var constraints = GridBagConstraints(0, rowCount, 1, 1, 0.0, 0.0, WEST, NONE, insets, 0, 0)
            if (showLabel(items)) {
                optionsPanel.add(JBLabel(label), constraints)
                columnCount++
            }
            items.forEachIndexed { index, item ->
                constraints = GridBagConstraints(columnCount++, rowCount, 1, 1, 0.0, 0.0, WEST, NONE, insets, 0, 0)
                if (index + 1 == items.size) {
                    constraints.gridwidth = maxColumns - columnCount + 1
                    constraints.weightx = 1.0
                    constraints.fill = HORIZONTAL
                    insets = JBUI.insets(0, 0, bottomInset, 0)
                }
                optionsPanel.add(item, constraints)
            }
            rowCount++
        }
        val insets = JBUI.insets(0, leftInset, bottomInset, 0)
        val constraints = GridBagConstraints(0, lineCount++, 1, 1, 1.0, 0.0, WEST, HORIZONTAL, insets, 0, 0)
        this.add(optionsPanel, constraints)
    }

    private fun groupingRadioButtons(radioButtons: Array<JBRadioButton>) {
        val buttonGroup = ButtonGroup()
        radioButtons.forEach { radioButton -> buttonGroup.add(radioButton) }
    }

    private fun message(key: String): String {
        return resourceBundle.getString("settings.JsonToPhpArray.$key")
    }
}