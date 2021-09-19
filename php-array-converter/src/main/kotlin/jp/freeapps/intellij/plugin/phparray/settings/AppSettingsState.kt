package jp.freeapps.intellij.plugin.phparray.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

/**
 * Supports storing the application settings in a persistent way.
 * The [State] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
    name = "jp.freeapps.intellij.plugin.phparray.settings.AppSettingsState",
    storages = [Storage("PhpArrayConverterPlugin.xml")]
)
class AppSettingsState : PersistentStateComponent<AppSettingsState?> {
    var replaceInEditor = true
    var useBraket = false
    var useDoubleQuote = false
    var appendComma = false

    override fun getState(): AppSettingsState {
        return this
    }

    override fun loadState(state: AppSettingsState) {
        XmlSerializerUtil.copyBean(state, this)
    }

    companion object {
        fun getInstance(): AppSettingsState =
            ApplicationManager.getApplication().getService(AppSettingsState::class.java)
    }
}