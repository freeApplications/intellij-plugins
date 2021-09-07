package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.json.psi.JsonFile
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import io.mockk.every
import io.mockk.mockkObject
import jp.freeapps.intellij.plugin.phparray.settings.AppSettingsState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class JsonConverterTest : LightJavaCodeInsightFixtureTestCase() {
    @Test
    fun testKeepFormat() {
        val json = """
[
  {"int" :12345, "float" :123.45, "string": "12345", "boolean" : true},
    { "array":[ 12345, 123.45, "12345", false ] },
      [   {   "key"   :   "value"   }   ]
]
"""
        val expectedDefault = """
array(
  array('int' =>12345, 'float' =>123.45, 'string'=> '12345', 'boolean' => true),
    array( 'array'=>array( 12345, 123.45, '12345', false ) ),
      array(   array(   'key'   =>   'value'   )   )
)
"""
        val expectedUseBraket = """
[
  ['int' =>12345, 'float' =>123.45, 'string'=> '12345', 'boolean' => true],
    [ 'array'=>[ 12345, 123.45, '12345', false ] ],
      [   [   'key'   =>   'value'   ]   ]
]
"""
        val expectedUseDoubleQuote = """
array(
  array("int" =>12345, "float" =>123.45, "string"=> "12345", "boolean" => true),
    array( "array"=>array( 12345, 123.45, "12345", false ) ),
      array(   array(   "key"   =>   "value"   )   )
)
"""
        val expectedUseBraketAndDoubleQuote = """
[
  ["int" =>12345, "float" =>123.45, "string"=> "12345", "boolean" => true],
    [ "array"=>[ 12345, 123.45, "12345", false ] ],
      [   [   "key"   =>   "value"   ]   ]
]
"""
        // Mocking AppSettingsState
        val appSettingsState = AppSettingsState()
        mockkObject(AppSettingsState)
        every { AppSettingsState.getInstance() } returns appSettingsState

        // useArray | useSingleQuote
        val jsonFile = createJsonFile(json)
        assertEquals(expectedDefault, JsonConverter(jsonFile).toPhpArray())

        // useBraket | useSingleQuote
        appSettingsState.useBraket = true
        assertEquals(expectedUseBraket, JsonConverter(jsonFile).toPhpArray())

        // useBraket | useDoubleQuote
        appSettingsState.useDoubleQuote = true
        assertEquals(expectedUseBraketAndDoubleQuote, JsonConverter(jsonFile).toPhpArray())

        // useArray | useDoubleQuote
        appSettingsState.useBraket = false
        assertEquals(expectedUseDoubleQuote, JsonConverter(jsonFile).toPhpArray())
    }

    @Test
    fun testSyntax() {
        val json = """
[{"[{":":"},[{"},[{" : " : ",	"\",\t\"":""}]]
"""
        val expectedDefault = """
array(array('[{'=>':'),array(array('},[{' => ' : ',	'",\t"'=>'')))
"""
        val expectedUseBraket = """
[['[{'=>':'],[['},[{' => ' : ',	'",\t"'=>'']]]
"""
        val expectedUseDoubleQuote = """
array(array("[{"=>":"),array(array("},[{" => " : ",	"\",\t\""=>"")))
"""
        val expectedUseBraketAndDoubleQuote = """
[["[{"=>":"],[["},[{" => " : ",	"\",\t\""=>""]]]
"""
        // Mocking AppSettingsState
        val appSettingsState = AppSettingsState()
        mockkObject(AppSettingsState)
        every { AppSettingsState.getInstance() } returns appSettingsState

        // useArray | useSingleQuote
        val jsonFile = createJsonFile(json)
        assertEquals(expectedDefault, JsonConverter(jsonFile).toPhpArray())

        // useBraket | useSingleQuote
        appSettingsState.useBraket = true
        assertEquals(expectedUseBraket, JsonConverter(jsonFile).toPhpArray())

        // useBraket | useDoubleQuote
        appSettingsState.useDoubleQuote = true
        assertEquals(expectedUseBraketAndDoubleQuote, JsonConverter(jsonFile).toPhpArray())

        // useArray | useDoubleQuote
        appSettingsState.useBraket = false
        assertEquals(expectedUseDoubleQuote, JsonConverter(jsonFile).toPhpArray())
    }

    @Test
    fun testEscapeSequence() {
        val json = """
[{	"\t"	:	
"\t:\t\n","\"'\\'\"":    "    "}]
"""
        val expectedDefault = """
array(array(	'\t'	=>	
'\t:\t\n','"\'\\\'"'=>    '    '))
"""
        val expectedUseBraket = """
[[	'\t'	=>	
'\t:\t\n','"\'\\\'"'=>    '    ']]
"""
        val expectedUseDoubleQuote = """
array(array(	"\t"	=>	
"\t:\t\n","\"'\\'\""=>    "    "))
"""
        val expectedUseBraketAndDoubleQuote = """
[[	"\t"	=>	
"\t:\t\n","\"'\\'\""=>    "    "]]
"""
        // Mocking AppSettingsState
        val appSettingsState = AppSettingsState()
        mockkObject(AppSettingsState)
        every { AppSettingsState.getInstance() } returns appSettingsState

        // useArray | useSingleQuote
        val jsonFile = createJsonFile(json)
        assertEquals(expectedDefault, JsonConverter(jsonFile).toPhpArray())

        // useBraket | useSingleQuote
        appSettingsState.useBraket = true
        assertEquals(expectedUseBraket, JsonConverter(jsonFile).toPhpArray())

        // useBraket | useDoubleQuote
        appSettingsState.useDoubleQuote = true
        assertEquals(expectedUseBraketAndDoubleQuote, JsonConverter(jsonFile).toPhpArray())

        // useArray | useDoubleQuote
        appSettingsState.useBraket = false
        assertEquals(expectedUseDoubleQuote, JsonConverter(jsonFile).toPhpArray())
    }

    @Test
    fun testAppendComma() {
        val json = """
[
  {
    "key": "value"
  },
  [
    "element"
  ],
  {}, [],
  {
    "key1": {
      "key": 123
    },
    "key2": {
      "key": [ 456, 789, ],
    },
    "key3": {
      "key": { "key": "value" }
    }
  }
]
"""
        val expected = """
array(
  array(
    'key'=> 'value',
  ),
  array(
    'element',
  ),
  array(), array(),
  array(
    'key1'=> array(
      'key'=> 123,
    ),
    'key2'=> array(
      'key'=> array( 456, 789, ),
    ),
    'key3'=> array(
      'key'=> array( 'key'=> 'value', ),
    ),
  ),
)
"""
        // Mocking AppSettingsState
        val appSettingsState = AppSettingsState()
        mockkObject(AppSettingsState)
        every { AppSettingsState.getInstance() } returns appSettingsState

        // appendComma
        appSettingsState.appendComma = true
        val jsonFile = createJsonFile(json)
        assertEquals(expected, JsonConverter(jsonFile).toPhpArray())
    }

    private fun createJsonFile(json: String): JsonFile {
        return myFixture.configureByText("target.json", json) as JsonFile
    }
}