/*
 * Copyright (c) 2019 Outsource Cafe, Inc. 
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

package com.javagen.schema.dart

import com.javagen.schema.model.MModule

import static com.javagen.schema.common.GlobalFunctionsUtil.*

/**
 * Dart language util functions, reserved words, etc.
 *
 * Style reference: https://www.dartlang.org/guides/language/effective-dart
 *
 * @author Richard Easterling
 */
class DartUtil {
    /**
     * Check for Dart reserved words.
     */
    static boolean isDartReservedWord(String ident)
    {
        return dartReservedWords.contains(ident)
    }
    static String trimSlashes(String path)
    {
        if (!path || path.isEmpty())
            return ''
        if (path?.startsWith('/'))
            path = path.substring(1)
        if (path?.endsWith('/'))
            path = path[0..-1]
        path
    }
//    static String toGeneratedRelativeFileName(String srcFolder, MModule m, String name, String suffix='.g', String subFolder=null)
//    {
//        String src = trimSlashes(m.fullName()?.replace('.', '/'))
//        srcFolder = trimSlashes(srcFolder)
//        subFolder = trimSlashes(subFolder)
//        if (name.endsWith('.dart'))
//            name = name[0..-6]
//        String result = "${src ? src+'/' : ''}${srcFolder ? srcFolder+'/' : ''}${subFolder ? subFolder + '/' : ''}${name}${suffix}.dart"
//        result
//    }
    static File toGeneratedSourceFileName(File src, String suffix='.g', String subFolder=null)
    {
        def name = src.name
        if (name.endsWith('.dart'))
            name = name[0..-6]
        subFolder = trimSlashes(subFolder)
        File result = new File("${src.parentFile.absolutePath}/${subFolder ? subFolder+'/' : ''}${name}${suffix}.dart")
        result
    }
    /**
     * Check for Dart default import class names.
     */
    static boolean isDartDefaultImportsClass(String ident)
    {
        return dartDefaultImportClasses.contains(ident)
    }
    static String digitToNumberName(int digit)
    {
        if (digit < 0)
            return "neg${Math.abs(digit)}"
        switch (digit) {
            case 0: return 'zero'
            case 1: return 'one'
            case 2: return 'two'
            case 3: return 'three'
            case 4: return 'four'
            case 5: return 'five'
            case 6: return 'six'
            case 7: return 'seven'
            case 8: return 'eight'
            case 9: return 'nine'
            case 0: return 'zero'
            default: return "ERROR digitToNumberName($digit)"
        }
    }

    /**
     * Make sure identifier is a legal Dart name and modify it if necessary.
     */
    static String legalDartName(String identifier)
    {
        if (identifier==null || identifier.trim().length()==0)
            return identifier
        if (Character.isDigit(identifier.charAt(0))) {
            final numberText = digitToNumberName(Integer.parseInt(identifier.substring(0,1)))
            return "${numberText}${upperCase(identifier.substring(1))}"
        }
        return isDartReservedWord(identifier) ? "a${upperCase(identifier)}" : identifier
    }

    /**
     * Make sure identifier is a legal Dart class name that doesn't clash with default imports and modify it if necessary.
     */
    static String legalDartClassName(String identifier, String removeSuffix='Type')
    {
        if (identifier==null || identifier.trim().length()==0)
            return identifier
        if (Character.isDigit(identifier.charAt(0))) {
            final numberText = upperCase(digitToNumberName(Integer.parseInt(identifier.substring(0,1))))
            identifier = "${numberText}${upperCase(identifier.substring(1))}"
        }
        if (removeSuffix && identifier.endsWith(removeSuffix)) {
            identifier = stripSuffix(identifier, removeSuffix)
        }
        identifier = upperCase(identifier)
        return isDartDefaultImportsClass(identifier) ? identifier+'_' : identifier
    }

    static String camelBackDartClass(String anyString)
    {
        return legalDartClassName( upperCase(camelBackName(anyString)) )
    }

    /**
     * Generate a legal uppercase or camelCase Dart enum name given an arbitrary string.
     */
    static String dartEnumName(String anyString, boolean allUpperCase=false, boolean preserveAcronymCase=false)
    {
        if (anyString==null || anyString.trim().length()==0)
            return null
        String normalized = replaceSpecialChars(anyString, ' ,_+-&/', (char)'_')
        if (allUpperCase)
            return legalDartName(normalized.toUpperCase())
        if (preserveAcronymCase && normalized.toUpperCase() == normalized)
            return legalDartName(normalized)
        return legalDartName(camelBackName(normalized))
    }

    /**
     * Generate a legal uppercase or camelCase Java enum name given an arbitrary string.
     */
    static String dartEnumValue(String anyString, boolean allUpperCase)
    {
        if (anyString==null || anyString.trim().length()==0)
            return null
        return escapeDartString( allUpperCase ? anyString.toUpperCase() : anyString )
    }

    /**
     * Convert arbitrary stirng to legal Dart constant name.  All non-legal
     * identifier characters are converted to '_'.
     */
    static String dartConstName(String anyString)
    {
        if (anyString == null)
            return null
        StringBuilder dartConst = new StringBuilder()
        int strlen = anyString.length()
        char lastChar = '\0'
        for(int i = 0; i < strlen; i++) {
            char c = Character.toUpperCase( anyString.charAt(i) )
            boolean validId = Character.isJavaIdentifierPart(c) && (c < 128) //close enough for now
            if (validId || lastChar != '_')
                dartConst.append( validId ? c : '_')
            lastChar = validId ? c : (char)'_'
        }
        return legalDartName( dartConst.toString() )
    }

    static String escapeDartString(String string) {
        if (!string)
            return string
        string = string.replace("\$","\\\$")
        string
    }



    ////////////////////////////////////////////////////////////////////////////
    // Dart language:
    ////////////////////////////////////////////////////////////////////////////

    static final String[] DART_RESERVED_WORDS_LIST = [
            //https://www.dartlang.org/guides/language/language-tour#keywords:
            'assert',
            'break',
            'case',
            'catch',
            'class',
            'const',
            'continue',
            'default',
            'do',
            'else',
            'enum',
            'extends',
            'false',
            'final',
            'finally',
            'for',
            'if',
            'in',
            'is',
            'new',
            'null',
            'rethrow',
            'return',
            'super',
            'switch',
            'this',
            'throw',
            'true',
            'try',
            'var',
            'void',
            'while',
            'with',
             //1 - contextual keywords
            'async',
            'hide',
            'on',
            'show',
            'sync',
            //2 - are built-in identifiers.
            'abstract',
            'as',
            'covariant',
            'deferred',
            'dynamic',
            'export',
            'external',
            'factory',
            'Function',
            'get',
            'implements',
            'import',
            'interface',
            'library',
            'mixin',
            'operator',
            'part',
            'set',
            'static',
            'typedef',
            //3 - asynchrony support
            'await',
            'yield'
    ]

    static final String[] DART_DEFAULT_CLASS_IMPORTS_LIST = [
            //https://api.dartlang.org/stable/2.1.0/dart-core/dart-core-library.html

            'BidirectionalIterator',
            'BigInt',
            'bool',
            'DateTime',
            'Deprecated',
            'Comparable',
            'double',
            'Duration',
            'Expando',
            'Function',
            'Future',
            'int',
            'Invocation',
            'Iterable',
            'Iterator',
            'List',
            'Map',
            'MapEntry',
            'Match',
            'Null',
            'num',
            'Object',
            'Pattern',
            'pragma',
            'Provisional',
            'RegExp',
            'RuneIterator',
            'Runes',
            'Set',
            'Sink',
            'StackTrace',
            'Stream',
            'Stopwatch',
            'String',
            'StringBuffer',
            'StringSink',
            'Symbol',
            'Type',
            'Uri',
            'UriData',
            //CONSTANTS
//            'deprecated',
//            'override',
//            'provisional',
//            'proxy',
            //FUNCTIONS
//            'identical',
//            'identityHashCode',
//            'print',
            //TYPEDEFS
            'Comparator',
            //EXCEPTIONS
            'AbstractClassInstantiationError',
            'ArgumentError',
            'AssertionError',
            'CastError',
            'ConcurrentModificationError',
            'CyclicInitializationError',
            'Error',
            'Exception',
            'FallThroughError',
            'FormatException',
            'IndexError',
            'IntegerDivisionByZeroException',
            'NoSuchMethodError',
            'NullThrownError',
            'OutOfMemoryError',
            'StackOverflowError',
            'StateError',
            'TypeError',
            'UnimplementedError',
            'UnsupportedError',
            //https://api.dartlang.org/stable/2.1.0/dart-async/dart-async-library.html
            'Future',
            'Stream',
    ]

    private static Set<String> dartReservedWords = Arrays.asList(DART_RESERVED_WORDS_LIST) as Set
    private static Set<String> dartDefaultImportClasses = Arrays.asList(DART_DEFAULT_CLASS_IMPORTS_LIST) as Set


}
