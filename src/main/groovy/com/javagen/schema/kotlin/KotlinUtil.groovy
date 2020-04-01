/*
 * Copyright (c) 2017 Outsource Cafe, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the 'License');
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.javagen.schema.kotlin

import static com.javagen.schema.common.GlobalFunctionsUtil.*

/**
 * Kotlin language util functions, reserved words, etc.
 *
 * @author Richard Easterling
 */
class KotlinUtil 
{
    /**
     * Check for Kotlin reserved words.
     */
    static boolean isKotlinReservedWord(String ident)
    {
        return kotlinReservedWords.contains(ident)
    }
    /**
     * Check for Kotlin default import class names.
     */
    static boolean isKotlinDefaultImportsClass(String ident)
    {
        return kotlinDefaultImportClasses.contains(ident)
    }
    /**
     * Make sure identifier is a legal Kotlin name and modify it if necessary.
     */
    static String legalKotlinName(String identifier)
    {
        if (identifier==null || identifier.trim().length()==0)
            return identifier
        if (Character.isDigit(identifier.charAt(0))) {
            return '_'+identifier
        }
        return isKotlinReservedWord(identifier) ? identifier+'_' : identifier
    }

    /**
     * Make sure identifier is a legal Kotlin class name that doesn't clash with default imports and modify it if necessary.
     */
    static String legalKotlinClassName(String identifier, String removeSuffix='Type')
    {
        if (identifier==null || identifier.trim().length()==0)
            return identifier
        if (Character.isDigit(identifier.charAt(0))) {
            identifier = '_'+identifier
        }
        if (removeSuffix && identifier.endsWith(removeSuffix)) {
            identifier = stripSuffix(identifier, removeSuffix)
        }
        identifier = upperCase(identifier)
        return isKotlinDefaultImportsClass(identifier) ? identifier+'_' : identifier
    }
    static String camelBackKotlinClass(String anyString)
    {
        return legalKotlinClassName( upperCase(camelBackName(anyString)) )
    }

    /**
     * Generate a legal uppercase or camelCase Kotlin enum name given an arbitrary string.
     */
    static String kotlinEnumName(String anyString, boolean allUpperCase)
    {
        if (anyString==null || anyString.trim().length()==0)
            return null
        String normalized = replaceSpecialChars(anyString)
        return allUpperCase ? legalKotlinName(normalized.toUpperCase()) : camelBackKotlinClass(normalized)
    }

    /**
     * Convert arbitrary stirng to legal Kotlin constant name.  All non-legal
     * identifier characters are converted to '_'.
     */
    static String kotlinConstName(String anyString)
    {
        if (anyString == null)
            return null
        StringBuilder kotlinConst = new StringBuilder()
        int strlen = anyString.length()
        char lastChar = '\0'
        for(int i = 0; i < strlen; i++) {
            char c = Character.toUpperCase( anyString.charAt(i) )
            boolean validId = Character.isJavaIdentifierPart(c) && (c < 128) //close enough for now
            if (validId || lastChar != '_')
                kotlinConst.append( validId ? c : '_')
            lastChar = validId ? c : (char)'_'
        }
        return legalKotlinName( kotlinConst.toString() )
    }


    ////////////////////////////////////////////////////////////////////////////
    // Kotlin language:
    ////////////////////////////////////////////////////////////////////////////

    static final String[] KOTLIN_RESERVED_WORDS_LIST = [
            //http://kotlinlang.org/docs/reference/keyword-reference.html#hard-keywords:
            'as',
            'break',
            'class',
            'continue',
            'do',
            'else',
            'false',
            'for',
            'fun',
            'if',
            'in',
            'interface',
            'is',
            'null',
            'object',
            'package',
            'return',
            'super',
            'this',
            'throw',
            'true',
            'try',
            'typealias',
            'val',
            'var',
            'when',
            'while'
            //'typeof' //??
            //http://kotlinlang.org/docs/reference/keyword-reference.html#soft-keywords
//            'by',
//            'catch',
//            'constructor',
//            'delegate',
//            'dynamic',
//            'field',
//            'file',
//            'finally',
//            'get',
//            'import',
//            'init',
//            'param',
//            'property',
//            'reciever',
//            'set',
//            'setparam',
//            'where'
            //http://kotlinlang.org/docs/reference/keyword-reference.html#modifier-keywords
//            'actual',
//            'abstract',
//            'annotation',
//            'companion',
//            'const',
//            'crossinline',
//            'data',
//            'enum',
//            'expect',
//            'external',
//            'final',
//            'infix',
//            'inline',
//            'inner',
//            'internal',
//            'lateinit',
//            'noinline',
//            'open',
//            'operator',
//            'out',
//            'override',
//            'private',
//            'protected',
//            'public',
//            'reified',
//            'sealed',
//            'suspend',
//            'tailrec',
//            'vararg',
            //http://kotlinlang.org/docs/reference/keyword-reference.html#special-identifiers
//            'field',
//            'it'
    ]

    static final String[] KOTLIN_DEFAULT_CLASS_IMPORTS_LIST = [

            //https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/index.html
            'Annotation',
            'Any',
            'Array',
            'AssertionError',
            'Boolean',
            'BooleanArray',
            'Byte',
            'ByteArray',
            'Char',
            'CharArray',
            'CharSequence',
            'ClassCastException',
            'Comparable',
            'Comparator',
            'ConcurrentModificationException',
            'DeprecationLevel',
            'Double',
            'DoubleArray',
            'Enum',
            'Error',
            'Exception',
            'Float',
            'FloatArray',
            'Function',
            'IllegalArgumentException',
            'IllegalStateException',
            'IndexOutOfBoundsException',
            'Int',
            'IntArray',
            'KotlinVersion',
            'Lazy',
            'LazyThreadSafetyMode',
            'Long',
            'LongArray',
            'NoSuchElementException',
            'NoWhenBranchMatchedException',
            'Nothing',
            'NullPointerException',
            'Number',
            'NumberFormatException',
            'Pair',
            'RuntimeException',
            'Short',
            'ShortArray',
            'String',
            'Throwable',
            'Triple',
            'UninitializedPropertyAccessException',
            'Unit',
            'UnsupportedOperationException',
            //https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/index.html
            'AbstractCollection',
            'AbstractIterator',
            'AbstractList',
            'AbstractMap',
            'AbstractMutableCollection',
            'AbstractMutableList',
            'AbstractMutableMap',
            'AbstractMutableSet',
            'AbstractSet',
            'ArrayList',
            'BooleanIterator',
            'ByteIterator',
            'CharIterator',
            'Collection',
            'DoubleIterator',
            'FloatIterator',
            'Grouping',
            'HashMap',
            'HashSet',
            'IndexedValue',
            'Iterator',
            'LinkedHashMap',
            'LinkedHashSet',
            'List',
            'ListIterator',
            'LongIterator',
            'Map',
            'MutableCollection',
            'MutableIterable',
            'MutableIterator',
            'MutableList',
            'MutableListIterator',
            'MutableMap',
            'MutableSet',
            'RandomAccess',
            'Set',
            'ShortIterator',
            //https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.sequences/index.html
            'Sequence',
            //https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.text/index.html
            'Appendable',
            'CharCategory',
            'CharDirectionality',
            'Charsets',
            'MatchGroup',
            'MatchGroupCollection',
            'MatchNamedGroupCollection',
            'MatchResult',
            'Regex',
            'RegexOption',
            'StringBuilder',
            'Typography',
    ]

    private static Set<String> kotlinReservedWords = Arrays.asList(KOTLIN_RESERVED_WORDS_LIST) as Set
    private static Set<String> kotlinDefaultImportClasses = Arrays.asList(KOTLIN_DEFAULT_CLASS_IMPORTS_LIST) as Set


}
