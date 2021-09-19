package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.jetbrains.php.lang.psi.PhpFile
import jp.freeapps.intellij.plugin.phparray.exception.ConvertException
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.ExpectedException
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import java.io.File

@RunWith(Enclosed::class)
internal class PhpArrayConverterTest {
    @RunWith(Parameterized::class)
    internal class ToJsonTest : LightJavaCodeInsightFixtureTestCase() {
        @Parameter
        lateinit var title: String

        @Parameter(1)
        lateinit var argument: String

        @Parameter(2)
        lateinit var expected: String

        @Test
        fun test() {
            val psiFile = myFixture.configureByText("target.php", argument)
            val phpArrayConverter = PhpArrayConverter(psiFile as PhpFile)

            // when:
            val actual = phpArrayConverter.toJson()

            // then:
            assertEquals(expected, actual)
        }

        companion object {
            @JvmStatic
            @Parameters(name = "{0}")
            fun dataProvider(): Iterable<Any> {
                return listOf(
                    createPattern("keepFormat", "useArray,useSingleQuote"),
                    createPattern("keepFormat", "useBraket,useSingleQuote"),
                    createPattern("keepFormat", "useArray,useDoubleQuote"),
                    createPattern("keepFormat", "useBraket,useDoubleQuote"),
                    createPattern("syntax", "useArray,useSingleQuote"),
                    createPattern("syntax", "useBraket,useSingleQuote"),
                    createPattern("syntax", "useArray,useDoubleQuote"),
                    createPattern("syntax", "useBraket,useDoubleQuote"),
                    createPattern("syntax", "all"),
                    createPattern("escapeSequence", "useArray,useSingleQuote"),
                    createPattern("escapeSequence", "useBraket,useSingleQuote"),
                    createPattern("escapeSequence", "useArray,useDoubleQuote"),
                    createPattern("escapeSequence", "useBraket,useDoubleQuote"),
                    createPattern("lastElementComma", "remove"),
                    createPattern("keyPattern", "all"),
                )
            }

            private fun createPattern(directory: String, pattern: String): Array<Any> {
                return arrayOf(
                    "$directory($pattern)",
                    loadResource(directory, pattern, false),
                    loadResource(directory, pattern, true),
                )
            }
        }
    }

    @RunWith(Parameterized::class)
    internal class CheckValidTypeTest : LightJavaCodeInsightFixtureTestCase() {
        @Parameter
        lateinit var title: String

        @Parameter(1)
        lateinit var argument: String

        @Parameter(2)
        lateinit var expected: String

        @get:Rule
        val thrown: ExpectedException? = ExpectedException.none()

        @Test
        fun test() {
            // setup:
            val psiFile = myFixture.configureByText("target.php", argument)
            val phpArrayConverter = PhpArrayConverter(psiFile as PhpFile)

            // expect:
            thrown!!.expect(ConvertException::class.java)
            thrown!!.expectMessage(expected)
            phpArrayConverter.toJson()
        }

        companion object {
            @JvmStatic
            @Parameters(name = "{0}")
            fun dataProvider(): Iterable<Any> {
                return listOf(
                    *createPatterns("syntaxError", 3),
                    *createPatterns("literalTypeError", 2),
                    *createPatterns("rootElementError", 2),
                )
            }

            private const val directory = "invalidType"
            private fun createPatterns(pattern: String, numberOfPatterns: Int): Array<Any> {
                return Array(numberOfPatterns) {
                    val number = it + 1
                    arrayOf(
                        "$directory($pattern#$number)",
                        loadResource("$directory/$pattern", "#$number", false),
                        loadResource("$directory/$pattern", "#$number", true),
                    )
                }
            }
        }
    }

    @Ignore
    companion object {
        private const val root = "src/test/resources/phpArrayToJson"
        private fun loadResource(directory: String, pattern: String, isExpected: Boolean): String {
            val name = if (isExpected) "expected" else "argument"
            val file = File("$root/$directory/$name($pattern).txt")
            if (file.exists()) return file.readText()
            return File("$root/$directory/$name.txt").readText()
        }
    }
}