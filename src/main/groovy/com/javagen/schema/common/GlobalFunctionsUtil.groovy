/*
 * Copyright (c) 2017 Outsource Cafe, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.javagen.schema.common

import com.sun.org.apache.xpath.internal.operations.Bool
import groovy.transform.CompileStatic
import org.xml.sax.InputSource

import org.xml.sax.helpers.DefaultHandler
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.SAXException

import java.util.function.Function


/**
 * Utility functions mostly targeting programming language string formatting.
 *
 * TODO move language-specific code into correct package namespaces.
 *
 * @author Richard Easterling
 */
@CompileStatic
final class GlobalFunctionsUtil
{
	////////////////////////////////////////////////////////////////////////////
	// Java common methods:
	////////////////////////////////////////////////////////////////////////////

	static final String[] JAVA_RESERVED_WORDS_LIST = [
        'abstract', 'assert', 'boolean', 'break', 'byte', 'byvalue', 'case', 'cast',
        'catch', 'char', 'class', 'const', 'continue', 'default', 'do', 'double',
        'else', 'enum', 'extends', 'false', 'final', 'finally', 'float', 'for', 'future',
        'generic', 'goto', 'if', 'implements', 'import', 'inner', 'instanceof',
        'int', 'interface', 'long', 'native', 'new', 'null', 'operator', 'outer',
        'package', 'private', 'protected', 'public', 'rest', 'return', 'short',
        'static', 'super', 'switch', 'synchronized', 'this', 'throw', 'throws',
        'transient', 'true', 'try', 'var', 'void', 'volatile', 'while',
    ]

	private static Set<String> javaReservedWords = Arrays.asList(JAVA_RESERVED_WORDS_LIST) as Set

	/**
	 * Check for Java reserved words.
	 */
	static boolean isJavaReservedWord(String ident)
	{
		return javaReservedWords.contains(ident)
	}

	static String camelBackJavaClass(String anyString)
	{
		String className = legalJavaName( upperCase(camelBackName(anyString)) )
		return className
	}

	/**
	 * Make sure identifier is a legal Java name and modify it if necessary.
	 * Assumes all characters pass the Character.isJavaIdentifierPart test.
	 */
	static String legalJavaName(String identifier)
	{
		if (identifier==null || identifier.trim().length()==0)
			return identifier

		if (!Character.isJavaIdentifierStart(identifier.charAt(0))) {
			return '_'+identifier
		}
		return isJavaReservedWord(identifier) ? identifier+'_' : identifier
	}

	static String javaVariableName(String anyString)
	{
		StringBuilder javaVariable = new StringBuilder()

		anyString.split('[ _-]').eachWithIndex { s, i ->

			if (i == 0) {
				javaVariable.append(s)
			} else {
				javaVariable.append(s.substring(0, 1).toUpperCase())
				javaVariable.append(s.substring(1, s.length()))
			}

		}
		
		return legalJavaName( javaVariable.toString() )
	}

	/**
	 * Generate a legal uppercase or camelCase Java enum name given an arbitrary string.
	 */
	static String javaEnumName(String anyString, boolean allUpperCase=false, boolean preserveAcronymCase=false)
	{
		if (anyString==null || anyString.trim().length()==0)
			return null
		String normalized = normalize(anyString)
		if (allUpperCase)
			return legalJavaName(normalized.toUpperCase())
		if (preserveAcronymCase && normalized.toUpperCase() == normalized)
			return legalJavaName(normalized)
		return legalJavaName(camelBackJavaClass(normalized))
	}

	/**
	 * Convert arbitrary stirng to legal Java constant name.  All non-legal
	 * identifier characters are converted to '_'.
	 */
	static String javaConstName(String anyString)
	{
		if (anyString == null)
			return null
		StringBuilder javaConst = new StringBuilder()
		int strlen = anyString.length()
		char lastChar = '\0'
		for(int i = 0; i < strlen; i++) {
			char c = Character.toUpperCase( anyString.charAt(i) )
			boolean validId = Character.isJavaIdentifierPart(c) && (c < 128)
			if (validId || lastChar != (char)'_')
				javaConst.append( validId ? c : '_')
			lastChar = validId ? c : (char)'_'
		}
		return legalJavaName( javaConst.toString() )
	}
	static String javaPackageFromNamespace(String namespace, boolean stripNumbers = false)
	{
		if (!namespace)
			return null
		boolean canReverse = true
		List<String> result = []
		namespace.split('[/:]').each { String seg ->
			if (seg && !seg.startsWith('http')) {
				String [] tokens = canReverse ? seg.split('\\.').reverse() : seg.split('\\.')
				tokens.each { String token ->
					if (token != 'www' && (!stripNumbers || !isAllDigits(token))) {
						String legal = legalJavaName(token.toLowerCase())
						if (legal)
							result << legal
					}
				}
				if (canReverse)
					canReverse = false //only allow reverse on domain segment
			}
		}
		result.join('.')
	}

	/**
	 * Add escapes to make legal Java regular expression.
	 */
	static String escapeJavaRegexp(String regexp) {
		if (!regexp)
			return regexp
		regexp = regexp.replace("\\","\\\\")
		(regexp.startsWith('^') ? '' : '^') + regexp + (regexp.endsWith('$') ? '' : '$')
	}

	static String pathFromPackage(String fullClassName, String ext = 'java')
	{
		if (!fullClassName)
			return null
		String filePath = fullClassName.replace('.','/')
		return "${filePath}.${ext}"
	}

	static String javadocEscape(String name)
	{
		name?.replace('<', '&lt;')
	}


	////////////////////////////////////////////////////////////////////////////
	// generic common methods:
	////////////////////////////////////////////////////////////////////////////

	/**
	 * Quirky relative path extractor.
	 * @param url can be relative or absolute, but if it has a dot, it's treated as relative.
	 * @return relative path if url maps to a file and is relative or starts with dot, otherwise returns null
	 */
	static String containsRelativeFilePath(URL url)
	{
		final String protocol = url.protocol
		if (protocol == 'file') {
			final String path = url.path
			final int index = path.indexOf('/.')
			if (index >= 0) {
				final String relative = path.substring(index+1)
				return relative
				//final Path path1 = Paths.get(url.toURI())
				//return path.toString()
			} else if (path.startsWith('.')) {
				return path
			} else if (!path.startsWith('/') && path.indexOf(':') == -1) { //excludes windows drive paths: C:/path
				return path
			}
		}
		null
	}

	private static class NamespaceHandler extends DefaultHandler
	{
		Map<String,String> namespaces = [:]
		Set<String> alreadyDefinedNS = [] as Set
		boolean targetFound = false
		String targetNamespaceKey = 'targetNamespace'

		NamespaceHandler() {
			namespaces['xml'] = "http://www.w3.org/XML/1998/namespace"
		}

		@Override void startPrefixMapping (String prefix, String uri) throws SAXException
		{
			//if ( ! alreadyDefinedNS.contains(uri) ) {
				alreadyDefinedNS << uri
				namespaces.put(prefix, uri)
			//}
		}
		@Override void startElement(String uri, String localName,String qName, Attributes attributes) throws SAXException
		{
			if (!targetFound && targetNamespaceKey!=null) {
				int index = attributes.getIndex(targetNamespaceKey)
				if (index >= 0) {
					namespaces[targetNamespaceKey] = attributes.getValue(index)
					println("<${qName} targetNamespace='${attributes.getValue(index)}'>")
				}
				targetFound = true
			}
		}
	}

//	static Map<String,String> overrideNamespaces(Map<String,String> namespaces, String overrideNamespace)
//	{
//		if (overrideNamespace) {
//			String targetNamespace = namespaces['targetNamespace']
//			if (!targetNamespace)
//				throw new IllegalStateException("no targetNamespace defined in ${namespaces}")
//			Map<String,String> result = new HashMap<>(namespaces)
//			result.keySet().forEach{ key ->
//				if (result[key.toString()] == targetNamespace ) {
//					result[key.toString()] = overrideNamespace
//				}
//			}
//			result
//		} else {
//			namespaces
//		}
//	}



	static Map<String,String>loadNamespaces(URL xmlUrl, Set<String> alreadyDefinedNS=new HashSet<>(), String targetNamespaceKey='targetNamespace')
	{
		final SAXParserFactory factory = SAXParserFactory.newInstance()
		factory.setNamespaceAware(true)
		SAXParser saxParser = factory.newSAXParser()
		NamespaceHandler handler = new NamespaceHandler(targetNamespaceKey:targetNamespaceKey, alreadyDefinedNS:alreadyDefinedNS)
		saxParser.parse(xmlUrl.toString(), handler)
		handler.namespaces
	}
	static Map<String,String>loadNamespaces(String xml, Set<String> alreadyDefinedNS=new HashSet<>(), String targetNamespaceKey='targetNamespace')
	{
		final SAXParserFactory factory = SAXParserFactory.newInstance()
		factory.setNamespaceAware(true)
		SAXParser saxParser = factory.newSAXParser()
		NamespaceHandler handler = new NamespaceHandler(targetNamespaceKey:targetNamespaceKey, alreadyDefinedNS:alreadyDefinedNS)
		saxParser.parse(new InputSource(new StringReader(xml)), handler)
		handler.namespaces
	}

	/**
	 * Change first character to lower case
	 */
	static String lowerCase(String text, boolean tailToLowerCase = false) {
		if (text) {
			final String head = text[0..0].toLowerCase()
			final String tail = text.length()==1 ? '' : tailToLowerCase ? text[1..-1].toLowerCase() : text[1..-1]
			head+tail
		} else {
			text
		}
	}
	/**
	 * Change first character to upper case
	 */
	static String upperCase(String text, boolean tailToLowerCase = false)
	{
		if (text) {
			final String head = text[0..0].toUpperCase()
			final String tail = text.length()==1 ? '' : tailToLowerCase ? text[1..-1].toLowerCase() : text[1..-1]
			head+tail
		} else {
			text
		}
	}

	final static String WHITESPACE_CHARS = ' .,_+-/\r\n\t\\'
	final static char TOKEN_DIVIDER_CHAR = (char)'_'
	final static Closure LEGAL_ID_FUNC = { Character ch -> Character.isJavaIdentifierPart(ch) }
	final static Map<Character,String> EXPLICIT_REPLACEMENTS = [
			((char)'&') : '_and_'
	]

	/**
	 * Given a descriptive text phrase, prepare it for converting it to a language-specific type or variable name, by tokenizing
	 * it's words, removing extra whitespace and illegal characters, while replacing specific characters with
	 * specific text or trimming leading/trailing whitespace.
	 * Example normalize(" - boy's summer camp & soccer! - ") -> 'boys_summer_camp_and_soccer'
	 * @param text a descriptive word or phrase suitable for conversion to a type or variable name.
	 * @param whitespaceChars these characters are replaced by a tokenDividerChar.
	 * @param tokenDividerChar character to replace specialChars with, defaults to '_'
	 * @param specificReplacements explicit replacement text, defaults to replacing '&' with '_and_'
	 * @param legalIdCharFunc strips characters that return false, defaults to Character.isJavaIdentifierPart(ch)
	 * @param trim if true, removes whitespace from beginning and ending of text.
	 * @return tokenized name with preserved case
	 */
	static String normalize(final String text,
							String whitespaceChars = WHITESPACE_CHARS,
							char tokenDividerChar = TOKEN_DIVIDER_CHAR,
							Map<Character,String> specificReplacements = EXPLICIT_REPLACEMENTS,
							Closure legalIdCharFunc = LEGAL_ID_FUNC,
							boolean trim = true)
	{
		if (text == null || text.trim().isEmpty())
			return ''
		StringBuilder result = new StringBuilder()
		String input = text;
		int strlen = input.length()
		//if any specialReplacements, do the replacements first
		if (!specificReplacements.isEmpty()) {
			for (int i = 0; i < strlen; i++) {
				final char ch = input.charAt(i)
				final String specialReplacement = specificReplacements[ch];
				if (specialReplacement != null) {
					result.append(specialReplacement)
				} else {
					result.append(ch)
				}
			}
			input = result.toString()
			result.setLength(0)
			strlen = input.length()
		}
		boolean replacedWhitespace = false
		for (int i = 0; i < strlen; i++) {
			final char ch = input.charAt(i)
			if (whitespaceChars.indexOf((int)ch) >= 0) {
				if (replacedWhitespace) //only want one consecutive replacement
					continue
				result.append(tokenDividerChar)
				replacedWhitespace = true
			} else if (legalIdCharFunc(ch)) {
				result.append(ch)
				replacedWhitespace = false
			}
			//otherwise ignore (remove) non-legal characters
		}
		if (trim) {
			while(result.length() > 0 && result.charAt(0) == tokenDividerChar) {
				result.deleteCharAt(0);
			}
			while(result.length() > 0 && result.charAt(result.length()-1) == tokenDividerChar) {
				result.deleteCharAt(result.length()-1);
			}
		}
		result.toString()
	}
	/**
	 * Replaces non-legal identifier characters with underline char. Just calls normalize with default arguments.
	 */
	static String replaceSpecialChars(final String text)
	{
		return normalize(text);
	}

	/**
	 * strips blank/special characters from a string and makes first char after
	 * a special character upper case (excluding the very first character of the
	 * string)
	 */
	static String camelBackName(String text) {
//		if (anyString == null)
//			return ""
//		StringBuilder retstr = new StringBuilder("")
//		String specialCharacters = " _/.,#'%-"
//		int strlen = anyString.length()
//		char[] onechar = new char[1]
//		boolean nextUpper = false
//		boolean firstChar = true
//
//		for (int i = 0; i < strlen; i++) {
//			onechar[0] = anyString.charAt(i)
//			String charString = new String(onechar)
//			if (specialCharacters.indexOf(charString) >= 0) {
//				nextUpper = charString != '\''
//			} else if (firstChar) {
//				retstr.append(charString.toLowerCase())
//				firstChar = false
//				nextUpper = false
//			} else if (nextUpper) {
//				retstr.append(charString.toUpperCase())
//				nextUpper = false
//			} else {
//				retstr.append(charString) //.toLowerCase()
//			}
//
//		}
		camelCaseName(text)
	}

	/**
	 * Tokenizes the text if it contains whitespace, then
	 * @param text
	 * @param upperCamelCase
	 * @param tailToLowerCase
	 * @param preserveAcronymsOfLength
	 * @return
	 */
	static String camelCaseName(String text, boolean upperCamelCase=false, boolean tailToLowerCase = false, int preserveAcronymsOfLength=0) {
		if (text == null || text.trim().isEmpty())
			return ''
		StringBuilder result = new StringBuilder()
		String[] tokens = normalize(text).split(''+TOKEN_DIVIDER_CHAR)
		for(String token in tokens) {
			if (token.length() <= preserveAcronymsOfLength && token.toUpperCase() == token) {
				result.append(token)
			} else {
				final boolean toUpperCase = result.length()>0 || upperCamelCase
				result.append( toUpperCase ? upperCase(token, tailToLowerCase) : lowerCase(token, tailToLowerCase))
			}
		}
		result.toString()
	}

	/** remove XML element and attribute namespace prefixes */
	static String stripNamespace(String name)
	{
		def pos = name ? name.indexOf(':') : -1
		pos>0 ? name[pos+1..-1] : name
	}

	/** returns XML element and attribute namespace prefix or null if it is not present */
	static String extractNamespacePrefix(String name)
	{
		def pos = name ? name.indexOf(':') : -1
		return pos>0 ? name[0..pos-1] : (pos==0 ? '' : null)
	}

	static String className(String nodeType, String removeSuffix='Type')
	{
		if (!nodeType)
			return null
		upperCase( stripNamespace( nodeType.equalsIgnoreCase(removeSuffix) ? nodeType : stripSuffix(nodeType, removeSuffix) ) )
	}

	static String enumClassName(String nodeType, String suffix='Enum')
	{
		if (nodeType==null || nodeType.trim().length()==0)
			return null
		final String normalized = normalize(nodeType)
		addSuffix( upperCase(normalized), suffix)
	}

	static String moduelFromNamespace(String namespace)
	{
		if (!namespace)
			return null
		String[] segments = namespace.split('[/:]')
		int index = segments.length-1
		while (isAllDigits(segments[index]))
			index--
		segments[index]
	}

	static boolean isAllDigits(String str)
	{
		for (int i = 0; i < str.length(); i++)
		{
			char ch = str.charAt(i)
			if ( !(Character.isDigit(ch) || ch == (char)'.' || ch == (char)'-' || ch == (char)'+') )
				return false
		}
		return true
	}

	static String stripSuffix(String text, String suffix)
	{
		if (!text || !suffix || text == suffix)
			return text
		text.endsWith(suffix) ? text.substring(0,text.length()-suffix.length()) : text
	}

	static String addSuffix(String text, String suffix)
	{
		if (!text || !suffix)
			return text
		text.endsWith(suffix) ? text : text+suffix
	}

	static String stripDecimals(String text)
	{
		if (!text)
			return text
		final int pos = text.indexOf('.')
		pos < 0 ? text : text.substring(0, pos)
	}

	static void printStackTrace()
	{
		new Throwable().printStackTrace()
	}

//	static String packageFromPath(String filePath)
//    {
//        if (filePath==null || filePath.length()==0)
//            return ''
//        filePath = filePath.replace('\\','/')
//        filePath = filePath.replace('/','.')
//        int startPos = filePath.startsWith('.') ? 1 : 0
//        int endPos = filePath.endsWith('.') ? filePath.length()-1 : filePath.length()
//        filePath.substring(startPos, endPos)
//    }

	/**
	 * uses singular-to-plural rules, but harder to do reverse process due to data loss and overlap.
	 * see: http://www.grammar.cl/Notes/Plural_Nouns.htm
	 */
	static String toSingular(String plural)
	{
		if (plural==null)
			return plural
		final int len = plural.length()
		if (plural.endsWith('ies')) {
			return plural.substring(0,len-3) + 'y'	//cities -> city,
		} else if (plural.endsWith('ves')) {
			return plural.substring(0,len-3) + 'f' // wolves -> wolf, lives #> life
		} else if (plural.endsWith('es')) {
			return plural.substring(0,len-2) // dishes -> dish, heroes -> hero
		} else if (plural.endsWith('s')) {
			return plural.substring(0,len-1)
		} else {
			return plural
		}
	}

	/**
	 * Apply plural rules. Can't guess irregular nouns.
	 *
	 * see: http://www.grammar.cl/Notes/Plural_Nouns.htm
	 */
	static String toPlural(String singular)
	{
		if (singular==null)
			return singular
		final int len = singular.length()
		if (singular.endsWith('s') || singular.endsWith('ch') || singular.endsWith('sh') || singular.endsWith('x') || singular.endsWith('z'))
			return singular + 'es' 			// bus -> buses, match -> matches, dish -> dishes, box -> boxes, quiz -> quizes
		else if (singular.endsWith('y')) {
			char c = len > 2 ?  singular.charAt(len - 2) : (char)'?' 		//2nd to last char
			if (charType(c) == CharType.vowel) {
				return singular + 's' 								//day -> days
			} else {
				return singular.substring(0, len - 1) + 'ies' 		//city -> cities
			}
		} else if (singular.endsWith('f')) {
			return singular.substring(0, len - 1) + 'ves'			// leaf -> leaves, wolf -> wolves
		} else if (singular.endsWith('fe')) {
			return singular.substring(0, len - 2) + 'ves'			// life -> lives, knife -> knives
		} else if (singular.endsWith('o')) {
				char c = len > 2 ? singular.charAt(len - 2) : (char)'a' 	//2nd to last char
				if (charType(c) == CharType.vowel) {
					return singular + 's' 							// zoo -> zoos, radio -> radios
				} else {
					return singular + 'es' 						// hero -> heroes, echo -> ehcoes
				}
		} else {
			return singular + 's'
		}
	}

	static boolean isSimpleString(String val) {
		if (val==null)
			return false
		for(char c : val.toCharArray()) {
			switch (charType(c)) {
				case CharType.system:
				case CharType.white:
				case CharType.symbol:
					return false
			}
		}
		true
	}

	enum CharType{constonant, vowel, digit, white, symbol, system }

	/** fast char categorization */
	static final CharType charType(char c)
	{
		switch (c) {
			case 0: return CharType.system
			case 1: return CharType.system
			case 2: return CharType.system
			case 3: return CharType.system
			case 4: return CharType.system
			case 5: return CharType.system
			case 6: return CharType.system
			case 7: return CharType.system
			case 8: return CharType.system
			case 9: return CharType.white
			case 10: return CharType.white
			case 11: return CharType.system
			case 12: return CharType.system
			case 13: return CharType.white
			case 14: return CharType.system
			case 15: return CharType.system
			case 16: return CharType.system
			case 17: return CharType.system
			case 18: return CharType.system
			case 19: return CharType.system
			case 20: return CharType.system
			case 21: return CharType.system
			case 22: return CharType.system
			case 23: return CharType.system
			case 24: return CharType.system
			case 25: return CharType.system
			case 26: return CharType.system
			case 27: return CharType.system
			case 28: return CharType.system
			case 29: return CharType.system
			case 30: return CharType.system
			case 31: return CharType.system
			case ' ': return CharType.white
			case '!': return CharType.symbol
			case '"': return CharType.symbol
			case '#': return CharType.symbol
			case '$': return CharType.symbol
			case '%': return CharType.symbol
			case '&': return CharType.symbol
			case '\'': return CharType.symbol
			case '(': return CharType.symbol
			case ')': return CharType.symbol
			case '*': return CharType.symbol
			case '+': return CharType.digit
			case ',': return CharType.symbol
			case '-': return CharType.symbol
			case '.': return CharType.digit
			case '/': return CharType.symbol
			case '0': return CharType.digit
			case '1': return CharType.digit
			case '2': return CharType.digit
			case '3': return CharType.digit
			case '4': return CharType.digit
			case '5': return CharType.digit
			case '6': return CharType.digit
			case '7': return CharType.digit
			case '8': return CharType.digit
			case '9': return CharType.digit
			case ':': return CharType.symbol
			case '': return CharType.symbol
			case '<': return CharType.symbol
			case '=': return CharType.symbol
			case '>': return CharType.symbol
			case '?': return CharType.symbol
			case '@': return CharType.symbol
			case 'A': return CharType.vowel
			case 'B': return CharType.constonant
			case 'C': return CharType.constonant
			case 'D': return CharType.constonant
			case 'E': return CharType.vowel
			case 'F': return CharType.constonant
			case 'G': return CharType.constonant
			case 'H': return CharType.constonant
			case 'I': return CharType.vowel
			case 'J': return CharType.constonant
			case 'K': return CharType.constonant
			case 'L': return CharType.constonant
			case 'M': return CharType.constonant
			case 'N': return CharType.constonant
			case 'O': return CharType.vowel
			case 'P': return CharType.constonant
			case 'Q': return CharType.constonant
			case 'R': return CharType.constonant
			case 'S': return CharType.constonant
			case 'T': return CharType.constonant
			case 'U': return CharType.vowel
			case 'V': return CharType.constonant
			case 'W': return CharType.constonant
			case 'X': return CharType.constonant
			case 'Y': return CharType.constonant
			case 'Z': return CharType.constonant
			case '[': return CharType.symbol
			case '\\': return CharType.symbol
			case ']': return CharType.symbol
			case '^': return CharType.symbol
			case '_': return CharType.symbol
			case '`': return CharType.symbol
			case 'a': return CharType.vowel
			case 'b': return CharType.constonant
			case 'c': return CharType.constonant
			case 'd': return CharType.constonant
			case 'e': return CharType.vowel
			case 'f': return CharType.constonant
			case 'g': return CharType.constonant
			case 'h': return CharType.constonant
			case 'i': return CharType.vowel
			case 'j': return CharType.constonant
			case 'k': return CharType.constonant
			case 'l': return CharType.constonant
			case 'm': return CharType.constonant
			case 'n': return CharType.constonant
			case 'o': return CharType.vowel
			case 'p': return CharType.constonant
			case 'q': return CharType.constonant
			case 'r': return CharType.constonant
			case 's': return CharType.constonant
			case 't': return CharType.constonant
			case 'u': return CharType.vowel
			case 'v': return CharType.constonant
			case 'w': return CharType.constonant
			case 'x': return CharType.constonant
			case 'y': return CharType.constonant
			case 'z': return CharType.constonant
			case '{': return CharType.symbol
			case '|': return CharType.symbol
			case '}': return CharType.symbol
			case '~': return CharType.symbol
			default:  return CharType.system
		}
	}

}
