package jp.freeapps.intellij.plugin.phparray.converter

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase
import com.jetbrains.php.lang.psi.PhpFile
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class PhpArrayConverterTest : LightJavaCodeInsightFixtureTestCase() {
    @Test
    fun testKeepFormat() {
        // useArray | useSingleQuote
        var phpArray = """<?php
array
(
  array('int' => 12345, 'float' => 123.45, 'string' => '12345', 'boolean' => true),
    array ( 'array' => array( 12345, 123.45, '12345', false ) ),
      array   (   array(   'key'   =>   'value'   )   )
)
;"""
        val expected = """<?php
[
  {"int" : 12345, "float" : 123.45, "string" : "12345", "boolean" : true},
    { "array" : [ 12345, 123.45, "12345", false ] },
      [   {   "key"   :   "value"   }   ]
]
;"""
        var phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // useBraket | useSingleQuote
        phpArray = """<?php
[
  ['int' => 12345, 'float' => 123.45, 'string' => '12345', 'boolean' => true],
    [ 'array' => [ 12345, 123.45, '12345', false ] ],
      [   [   'key'   =>   'value'   ]   ]
]
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // useBraket | useDoubleQuote
        phpArray = """<?php
[
  ["int" => 12345, "float" => 123.45, "string" => "12345", "boolean" => true],
    [ "array" => [ 12345, 123.45, "12345", false ] ],
      [   [   "key"   =>   "value"   ]   ]
]
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // useArray | useDoubleQuote
        phpArray = """<?php
array(
  array("int" => 12345, "float" => 123.45, "string" => "12345", "boolean" => true),
    array( "array" => array( 12345, 123.45, "12345", false ) ),
      array(   array(   "key"   =>   "value"   )   )
)
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())
    }

    @Test
    fun testSyntax() {
        // useArray | useSingleQuote
        var phpArray = """<?php
array(array('array(array('=>'=>'),array(array('),array(array('=> '=> ',	'",\t"' => '')))
;"""
        var expected = """<?php
[{"array(array(":"=>"},[{"),array(array(": "=> ",	"\",\t\"" : ""}]]
;"""
        var phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // useBraket | useSingleQuote
        phpArray = """<?php
[['[['=>'=>'],[['],[['=> '=> ',	'",\t"' => '']]]
;"""
        expected = """<?php
[{"[[":"=>"},[{"],[[": "=> ",	"\",\t\"" : ""}]]
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // useBraket | useDoubleQuote
        phpArray = """<?php
[["[["=>"=>"],[["],[["=> "=> ",	"\",\t\"" => ""]]]
;"""
        expected = """<?php
[{"[[":"=>"},[{"],[[": "=> ",	"\",\t\"" : ""}]]
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // useArray | useDoubleQuote
        phpArray = """<?php
array(array("array(array("=>"=>"),array(array("),array(array("=> "=> ",	"\",\t\"" => "")))
;"""
        expected = """<?php
[{"array(array(":"=>"},[{"),array(array(": "=> ",	"\",\t\"" : ""}]]
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // all
        phpArray = """<?php
array(["array(["=>'=>'],[array("],[array("=> '=> ',	"\",\t\"" => "")])
;"""
        expected = """<?php
[{"array([":"=>"},[{"],[array(": "=> ",	"\",\t\"" : ""}]]
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())
    }

    @Test
    fun testEscapeSequence() {
        // useArray | useSingleQuote
        var phpArray = """<?php
array(array(	'\t'	=>	
'\t:\t\n','"\'\\\'"'=>    '    '))
;"""
        val expected = """<?php
[{	"\t"	:	
"\t:\t\n","\"'\\'\"":    "    "}]
;"""
        var phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // useBraket | useSingleQuote
        phpArray = """<?php
[[	'\t'	=>	
'\t:\t\n','"\'\\\'"'=>    '    ']]
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // useBraket | useDoubleQuote
        phpArray = """<?php
[[	"\t"	=>	
"\t:\t\n","\"'\\'\""=>    "    "]]
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())

        // useArray | useDoubleQuote
        phpArray = """<?php
array(array(	"\t"	=>	
"\t:\t\n","\"'\\'\""=>    "    "))
;"""
        phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())
    }

    @Test
    fun testRemoveComma() {
        val phpArray = """<?php
array(
  array(
    'key' => 'value',
  ),
  array(
    'element',
  ),
  array(), array(),
  array(
    'key1' => array(
      'key' => 123,
    ),
    'key2' => array(
      'key' => array( 456, 789, ),
    ),
    'key3' => array(
      'key' => array( 'key' => 'value', ),
    ),
  ),
)
;"""
        val expected = """<?php
[
  {
    "key" : "value"
  },
  [
    "element"
  ],
  [], [],
  {
    "key1" : {
      "key" : 123
    },
    "key2" : {
      "key" : [ 456, 789 ]
    },
    "key3" : {
      "key" : { "key" : "value" }
    }
  }
]
;"""
        val phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())
    }

    @Test
    fun testKeyPattern() {
        val phpArray = """<?php
array(
    1     => 'a',
    '1'   => 'b',
    1.5   => 'c',
    -1    => 'd',
    '01'  => 'e',
    '1.5' => 'f',
    true  => 'g',
    false => 'h',
    ''    => 'i',
    null  => 'j',
    'k',
    2     => 'l',
)
;"""
        val expected = """<?php
{
    "1"     : "a",
    "1"   : "b",
    "1.5"   : "c",
    "-1"    : "d",
    "01"  : "e",
    "1.5" : "f",
    "true"  : "g",
    "false" : "h",
    ""    : "i",
    "null"  : "j",
    "2":"k",
    "2"     : "l"
}
;"""
        val phpFile = createPhpFile(phpArray)
        assertEquals(expected, PhpArrayConverter(phpFile).toJson())
    }

    private fun createPhpFile(phpArray: String): PhpFile {
        return myFixture.configureByText("target.php", phpArray) as PhpFile
    }
}