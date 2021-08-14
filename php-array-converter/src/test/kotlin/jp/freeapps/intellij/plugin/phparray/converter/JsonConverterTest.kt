package jp.freeapps.intellij.plugin.phparray.converter

import org.junit.Test
import kotlin.test.assertEquals

internal class JsonConverterTest {
    @Test
    fun testFormat() {
        val json = """
[
  {"int" :12345, "float" :123.45, "string": "12345", "boolean" : true},
    { "array":[ 12345, 123.45, "12345", false ] },
      [   {   "key"   :   "value"   }   ]
]
"""
        val expectedDefault = """
array(
  array('int' => 12345, 'float' => 123.45, 'string' => '12345', 'boolean' => true),
    array( 'array' => array( 12345, 123.45, '12345', false ) ),
      array(   array(   'key'   =>   'value'   )   )
)
"""
        val expectedUseBraket = """
[
  ['int' => 12345, 'float' => 123.45, 'string' => '12345', 'boolean' => true],
    [ 'array' => [ 12345, 123.45, '12345', false ] ],
      [   [   'key'   =>   'value'   ]   ]
]
"""
        val expectedUseDoubleQuote = """
array(
  array("int" => 12345, "float" => 123.45, "string" => "12345", "boolean" => true),
    array( "array" => array( 12345, 123.45, "12345", false ) ),
      array(   array(   "key"   =>   "value"   )   )
)
"""
        val expectedUseBraketAndDoubleQuote = """
[
  ["int" => 12345, "float" => 123.45, "string" => "12345", "boolean" => true],
    [ "array" => [ 12345, 123.45, "12345", false ] ],
      [   [   "key"   =>   "value"   ]   ]
]
"""
        assertEquals(
            expectedDefault,
            JsonConverter(json).toPhpArray()
        )
        assertEquals(
            expectedUseBraket,
            JsonConverter(json).setUseBraket(true).toPhpArray()
        )
        assertEquals(
            expectedUseDoubleQuote,
            JsonConverter(json).setUseDoubleQuote(true).toPhpArray()
        )
        assertEquals(
            expectedUseBraketAndDoubleQuote,
            JsonConverter(json).setUseBraket(true).setUseDoubleQuote(true).toPhpArray()
        )
    }

    @Test
    fun testSyntax() {
        val json = """
[{"[{":":"},[{"},[{" : " : ",	"\",\t\"":""}]]
"""
        val expectedDefault = """
array(array('[{' => ':'),array(array('},[{' => ' : ',	'",\t"' => '')))
"""
        val expectedUseBraket = """
[['[{' => ':'],[['},[{' => ' : ',	'",\t"' => '']]]
"""
        val expectedUseDoubleQuote = """
array(array("[{" => ":"),array(array("},[{" => " : ",	"\",\t\"" => "")))
"""
        val expectedUseBraketAndDoubleQuote = """
[["[{" => ":"],[["},[{" => " : ",	"\",\t\"" => ""]]]
"""
        assertEquals(
            expectedDefault,
            JsonConverter(json).toPhpArray()
        )
        assertEquals(
            expectedUseBraket,
            JsonConverter(json).setUseBraket(true).toPhpArray()
        )
        assertEquals(
            expectedUseDoubleQuote,
            JsonConverter(json).setUseDoubleQuote(true).toPhpArray()
        )
        assertEquals(
            expectedUseBraketAndDoubleQuote,
            JsonConverter(json).setUseBraket(true).setUseDoubleQuote(true).toPhpArray()
        )
    }

    @Test
    fun testEscapeSequence() {
        val json = """
[{	"\t"	:	
"\t:\t\n","\"'\\'\"":    "    "}]
"""
        val expectedDefault = """
array(array(	'\t'	 => 	
'\t:\t\n','"\'\\\'"' =>    '    '))
"""
        val expectedUseBraket = """
[[	'\t'	 => 	
'\t:\t\n','"\'\\\'"' =>    '    ']]
"""
        val expectedUseDoubleQuote = """
array(array(	"\t"	 => 	
"\t:\t\n","\"'\\'\"" =>    "    "))
"""
        val expectedUseBraketAndDoubleQuote = """
[[	"\t"	 => 	
"\t:\t\n","\"'\\'\"" =>    "    "]]
"""
        assertEquals(
            expectedDefault,
            JsonConverter(json).toPhpArray()
        )
        assertEquals(
            expectedUseBraket,
            JsonConverter(json).setUseBraket(true).toPhpArray()
        )
        assertEquals(
            expectedUseDoubleQuote,
            JsonConverter(json).setUseDoubleQuote(true).toPhpArray()
        )
        assertEquals(
            expectedUseBraketAndDoubleQuote,
            JsonConverter(json).setUseBraket(true).setUseDoubleQuote(true).toPhpArray()
        )
    }
}