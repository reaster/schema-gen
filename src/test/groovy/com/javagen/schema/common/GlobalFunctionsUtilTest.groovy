package com.javagen.schema.common

import org.junit.Test

import static com.javagen.schema.common.GlobalFunctionsUtil.*
import static junit.framework.TestCase.*

class GlobalFunctionsUtilTest
{

    @Test
    void testNormalize()
    { //  ,_+-&/
        assertEquals('a', normalize('__a__'))
        assertEquals('a_b_c', normalize(', -a  -+b/_ _,c  '))
        assertEquals('a_and_b', normalize('a & b'))
        assertEquals('B_and_B', normalize('B&B'))
        assertEquals('Jims_stuff', normalize("Jim's stuff"))
        assertEquals('hey_dude', normalize('"?hey dude!\"'))
    }

    @Test
    void testIsSimpleString()
    {
        assertTrue('alphanumeric', isSimpleString('heyDude'))
        assertTrue('alphanumeric', isSimpleString('heyDude123'))
        assertFalse('symbols', isSimpleString('hey!dude'))
        assertFalse('white space', isSimpleString('hey dude'))
        assertFalse('null', isSimpleString(null))
    }
//    @Test
//    void testCamelBackName()
//    {
//        assertEquals('handle leading special chars','heyDude', camelBackName('_Hey_dude'))
//        assertEquals('leave embedded upper case','searchTag1', camelBackName('searchTag1'))
//        assertEquals('remove multiple spaces','heyDude', camelBackName('hey  dude'))
//        assertEquals('lowercase first char','heyDude', camelBackName('Hey_dude'))
//    }

    @Test
    void testCamelCaseName()
    {
        assertEquals('upperCamelCase=true, tailToLowerCase=true, preserveAcronymsOfLength=2','IOStream', camelCaseName('IO STREAM', true, true, 2))
        assertEquals('handle leading special chars','heyDude', camelCaseName('_Hey_dude'))
        assertEquals('leave embedded upper case','searchTag1', camelCaseName('searchTag1'))
        assertEquals('remove multiple spaces','heyDude', camelCaseName('hey  dude'))
        assertEquals('lowercase first char','heyDude', camelCaseName('Hey_dude'))
        assertEquals('upperCamelCase=true','HeyDude', camelCaseName('Hey_dude', true))
        assertEquals('upperCamelCase=true, tailToLowerCase=true','HttpConnectionInfo', camelCaseName('HTTP connection info', true, true))
        assertEquals('upperCamelCase=true, tailToLowerCase=true, preserveAcronymsOfLength=2','HttpRequest', camelCaseName('HTTP REQUEST', true, true, 2))
        assertEquals('upperCamelCase=true, tailToLowerCase=true, preserveAcronymsOfLength=2','HTTPRequest', camelCaseName('HTTP REQUEST', true, true, 4))
        assertEquals('upperCamelCase=true, tailToLowerCase=true, preserveAcronymsOfLength=2','DB', camelCaseName('DB', true, true, 2))
        assertEquals('upperCamelCase=true, tailToLowerCase=true','Id', camelCaseName('ID', true, true))
    }

    @Test
    void testContainsRelativeFilePath()
    {
        assertEquals('if dot in path, assumes it was originally relative','./src/test/resources/xml.xsd', containsRelativeFilePath(new File('./src/test/resources/xml.xsd').toURI().toURL()))
        assertEquals('if dot in path, assumes it was originally relative','./src/test/resources/xml.xsd', containsRelativeFilePath(new URL('file:/Users/dude/./src/test/resources/xml.xsd')))
        assertEquals('./src/test/resources/xml.xsd', containsRelativeFilePath(new URL('file:./src/test/resources/xml.xsd')))
        assertEquals('src/test/resources/xml.xsd', containsRelativeFilePath(new URL('file:src/test/resources/xml.xsd')))
        assertNull('ignores windows paths', containsRelativeFilePath(new URL('file:C:src/test/resources/xml.xsd')))
    }

    @Test
    void testLoadNamespaces1()
    {
        Map<String,String> ns = loadNamespaces(new File('src/test/resources/xml.xsd').toURI().toURL())
        assertEquals('http://www.w3.org/2001/XMLSchema', ns['xs'])
        assertEquals('http://www.w3.org/XML/1998/namespace', ns['targetNamespace'])
        assertEquals('http://www.w3.org/XML/1998/namespace', ns['xml'])
    }
    @Test
    void testLoadNamespaces()
    {
        Map<String,String> ns = loadNamespaces(new File('src/test/resources/ns.xsd').toURI().toURL())
        assertEquals('http://www.w3.org/2001/XMLSchema', ns['xs'])
        assertEquals('urn:oasis:names:tc:ciq:xsdschema:xAL:2.0', ns[''])
        assertEquals('urn:oasis:names:tc:ciq:xsdschema:xAL:2.0', ns['targetNamespace'])
    }
    @Test
    void testJavaPackageFromNamespace()
    {
        assertEquals('urn.oasis.names.tc.ciq.xsdschema.xal._2._0', javaPackageFromNamespace('urn:oasis:names:tc:ciq:xsdschema:xAL:2.0'))
        assertEquals('urn.oasis.names.tc.ciq.xsdschema.xal', javaPackageFromNamespace('urn:oasis:names:tc:ciq:xsdschema:xAL:2.0', true))
        assertEquals('com.topografix.gpx._1._1', javaPackageFromNamespace('http://www.topografix.com/GPX/1/1'))
        assertEquals('com.topografix.gpx', javaPackageFromNamespace('http://www.topografix.com/GPX/1/1', true))
        assertEquals('org.w3._2001.xmlschema', javaPackageFromNamespace('http://www.w3.org/2001/XMLSchema'))
        assertEquals('org.w3.xmlschema', javaPackageFromNamespace('http://www.w3.org/2001/XMLSchema', true))
    }
    @Test
    void testAllDigits()
    {
        assertFalse( isAllDigits('w3') )
        assertTrue( isAllDigits('2.0') )
        assertFalse( isAllDigits('abc') )
    }
    @Test
    void testModuelFromNamespace()
    {
        assertEquals('xAL', moduelFromNamespace('urn:oasis:names:tc:ciq:xsdschema:xAL:2.0'))
        assertEquals('XMLSchema', moduelFromNamespace('http://www.w3.org/2001/XMLSchema'))
    }
    @Test
    void testExtractNamespacePrefix()
    {
        assertEquals('', extractNamespacePrefix(':name'))
        assertNull( extractNamespacePrefix('name') )
        assertEquals('p', extractNamespacePrefix('p:name'))
        assertEquals('prefix', extractNamespacePrefix('prefix:name'))
        assertEquals('prefix', extractNamespacePrefix('prefix:'))
    }

    @Test
    void testLowerCase()
    {
        assertNull(lowerCase(null))
        assertEquals('', lowerCase(''))
        assertEquals('a', lowerCase('A'))
        assertEquals('ab', lowerCase('Ab'))
        assertEquals('aB', lowerCase('AB'))
        assertEquals('ab', lowerCase('AB', true))
        assertEquals('a', lowerCase('A', true))
    }

    @Test
    void testUpperCase()
    {
        assertNull(upperCase(null))
        assertEquals('', upperCase(''))
        assertEquals('A', upperCase('a'))
        assertEquals('Ab', upperCase('ab'))
        assertEquals('AB', upperCase('AB'))
        assertEquals('Ab', upperCase('AB', true))
        assertEquals('A', upperCase('A', true))
    }

    @Test
    void testSingular()
    {
        //FAIL on nouns that end in 'e': assertEquals("picture", toSingular("pictures"))
        assertEquals("echo", toSingular("echoes"))
        assertEquals("bus", toSingular("buses"))
        assertEquals("match", toSingular("matches"))
        assertEquals("wolf", toSingular("wolves"))
    }

    @Test
    void testPlural()
    {
        assertEquals("pictures", toPlural("picture"))
        assertEquals("buses", toPlural("bus"))
        assertEquals("matches", toPlural("match"))
        assertEquals("dishes", toPlural("dish"))
        assertEquals("boxes", toPlural("box"))
        assertEquals("quizes", toPlural("quiz"))
        assertEquals("days", toPlural("day"))
        assertEquals("cities", toPlural("city"))
        assertEquals("wolves", toPlural("wolf"))
        assertEquals("leaves", toPlural("leaf"))
        assertEquals("radios", toPlural("radio"))
        assertEquals("zoos", toPlural("zoo"))
        assertEquals("echoes", toPlural("echo"))
        assertEquals("heroes", toPlural("hero"))
    }

    @Test
    void testJavaVariableName()
    {
        assertEquals("ab", javaVariableName("ab"))
        assertEquals("aB", javaVariableName("aB"))
        assertEquals("aBc", javaVariableName("aBc"))
        assertEquals("aB", javaVariableName("a-b"))
        assertEquals("aB", javaVariableName("a_b"))
        assertEquals("aB", javaVariableName("a b"))
    }

}
