package jp.freeapps.intellij.plugin.phparray.settings

import com.intellij.openapi.options.Configurable
import org.jetbrains.annotations.Nls
import javax.swing.JComponent

/**
 * Provides controller functionality for application settings.
 */
class AppSettingsConfigurable : Configurable {
    private var settingsComponent: AppSettingsComponent? = null

    override fun getDisplayName(): @Nls(capitalization = Nls.Capitalization.Title) String {
        return "PHP Array Converter"
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return settingsComponent!!.preferredFocusedComponent
    }

    override fun createComponent(): JComponent {
        settingsComponent = AppSettingsComponent()
        return settingsComponent as AppSettingsComponent
    }

    override fun isModified(): Boolean {
        val settings = AppSettingsState.getInstance()
        return settingsComponent!!.useBraket != settings.useBraket
                || settingsComponent!!.useDoubleQuote != settings.useDoubleQuote
                || settingsComponent!!.appendComma != settings.appendComma
    }

    override fun apply() {
        val settings = AppSettingsState.getInstance()
        settings.useBraket = settingsComponent!!.useBraket
        settings.useDoubleQuote = settingsComponent!!.useDoubleQuote
        settings.appendComma = settingsComponent!!.appendComma
    }

    override fun reset() {
        val settings = AppSettingsState.getInstance()
        settingsComponent!!.useBraket = settings.useBraket
        settingsComponent!!.useDoubleQuote = settings.useDoubleQuote
        settingsComponent!!.appendComma = settings.appendComma
    }

    override fun disposeUIResources() {
        settingsComponent = null
    }
}