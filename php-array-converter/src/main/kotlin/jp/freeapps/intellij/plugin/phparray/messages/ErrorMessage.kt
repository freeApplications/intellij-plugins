package jp.freeapps.intellij.plugin.phparray.messages

import com.intellij.openapi.util.TextRange
import java.util.*

class ErrorMessage(messageKey: String, textRange: TextRange, private val additionalText: String? = null) {
    private val resourceBundle = ResourceBundle.getBundle("messages/PluginBundle")

    val startOffset: Int = textRange.startOffset
    val endOffset: Int = textRange.endOffset
    val message: String = resourceBundle.getString(messageKey)
        get() = if (additionalText == null) field else "$field ( $additionalText )"
}