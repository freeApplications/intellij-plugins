package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.json.psi.JsonFile
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import io.mockk.every
import io.mockk.mockkObject
import jp.freeapps.intellij.plugin.phparray.settings.AppSettingsState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Parameterized::class)
internal class JsonConverterTest : LightJavaCodeInsightFixtureTestCase() {
    @Parameter
    lateinit var title: String

    @Parameter(1)
    lateinit var settings: AppSettingsState

    @Parameter(2)
    lateinit var argument: String

    @Parameter(3)
    lateinit var expected: String

    @Test
    fun test() {
        // setup:
        mockkObject(AppSettingsState)
        every { AppSettingsState.getInstance() } returns settings
        val psiFile = myFixture.configureByText("target.json", argument)
        val jsonConverter = JsonConverter(psiFile as JsonFile)

        // when:
        val actual = jsonConverter.toPhpArray()

        // then:
        assertEquals(expected, actual)
    }

    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun dataProvider(): Iterable<Any> {
            return listOf(
                createPattern("keepFormat", "useArray,useSingleQuote", false, false),
                createPattern("keepFormat", "useBraket,useSingleQuote", true, false),
                createPattern("keepFormat", "useArray,useDoubleQuote", false, true),
                createPattern("keepFormat", "useBraket,useDoubleQuote", true, true),
                createPattern("syntax", "useArray,useSingleQuote", false, false),
                createPattern("syntax", "useBraket,useSingleQuote", true, false),
                createPattern("syntax", "useArray,useDoubleQuote", false, true),
                createPattern("syntax", "useBraket,useDoubleQuote", true, true),
                createPattern("escapeSequence", "useArray,useSingleQuote", false, false),
                createPattern("escapeSequence", "useBraket,useSingleQuote", true, false),
                createPattern("escapeSequence", "useArray,useDoubleQuote", false, true),
                createPattern("escapeSequence", "useBraket,useDoubleQuote", true, true),
                createPattern("lastElementComma", "append", false, false, true),
                createPattern("lastElementComma", "remove", false, false, false),
            )
        }

        private fun createPattern(
            directory: String,
            pattern: String,
            useBraket: Boolean,
            useDoubleQuote: Boolean,
            appendComma: Boolean = false
        ): Array<Any> {
            val settings = AppSettingsState()
            settings.useBraket = useBraket
            settings.useDoubleQuote = useDoubleQuote
            settings.appendComma = appendComma
            return arrayOf(
                "$directory($pattern)",
                settings,
                loadResource(directory, pattern, false),
                loadResource(directory, pattern, true),
            )
        }

        private const val root = "src/test/resources/jsonToPhpArray"
        private fun loadResource(directory: String, pattern: String, isExpected: Boolean): String {
            val name = if (isExpected) "expected" else "argument"
            val file = File("$root/$directory/$name($pattern).txt")
            if (file.exists()) return file.readText()
            return File("$root/$directory/$name.txt").readText()
        }
    }
}