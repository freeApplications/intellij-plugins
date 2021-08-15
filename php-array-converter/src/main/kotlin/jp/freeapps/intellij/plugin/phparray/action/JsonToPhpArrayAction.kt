package jp.freeapps.intellij.plugin.phparray.action

import jp.freeapps.intellij.plugin.phparray.converter.JsonConverter

/**
 * Menu action to replace a selection of characters with a php array string.
 *
 * @see BaseAction
 */
class JsonToPhpArrayAction : BaseAction() {
    /**
     * Replace the selected text with php array string.
     */
    override fun replaceSelectedText(selectedText: String): String {
        val jsonConverter = JsonConverter(selectedText)
        return jsonConverter.toPhpArray()
    }

    /**
     * Determine if the selected text is a valid json string.
     */
    override fun isValid(selectedText: String): Boolean {
        val jsonConverter = JsonConverter(selectedText)
        return jsonConverter.isValid()
    }
}