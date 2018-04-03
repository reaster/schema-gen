/*
 * Copyright (c) 2018 Outsource Cafe, Inc.
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

package com.javagen.schema.swift

import static com.javagen.schema.common.PluralService.*
import static com.javagen.schema.common.GlobalFunctionsUtil.*

class SwiftUtil
{

    ////////////////////////////////////////////////////////////////////////////
    // Swift common methods:
    ////////////////////////////////////////////////////////////////////////////

    static final String[] SWIFT_RESERVED_WORDS_LIST = [
            //Keywords used in declarations:
            'associatedtype', 'class', 'deinit', 'enum', 'extension', 'fileprivate', ' func', 'import', 'init', 'inout', 'internal',
            'let', 'open', 'operator', 'private', 'protocol', 'public', 'static', 'struct', 'subscript', 'typealias', 'var',
            //Keywords used in statements:
            'break', 'case', 'continue', 'default', 'defer', 'do', 'else', 'fallthrough', 'for', 'guard', 'if', 'in', 'repeat',
            'return', 'switch', 'where', 'while',
            //Keywords used in expressions and types:
            'as', 'Any', 'catch', 'false', 'is', 'nil', 'rethrows', 'super', 'self', 'Self', 'throw', 'throws', 'true', 'try'
            //Keywords used in patterns: _.
            //Keywords that begin with a number sign (#):
            //#available, #colorLiteral, #column, #else, #elseif, #endif, #file, #fileLiteral, #function, #if, #imageLiteral, #line, #selector, and #sourceLocation.
            //Keywords reserved in particular contexts:
            //associativity, convenience, dynamic, didSet, final, get, infix, indirect, lazy, left, mutating, none, nonmutating,
            //optional, override, postfix, precedence, prefix, Protocol, required, right, set, Type, unowned, weak, and willSet. Outside the context in which they appear in the grammar, they can be used as identifiers.
    ]

    private static Set<String> swiftReservedWords = Arrays.asList(SWIFT_RESERVED_WORDS_LIST) as Set

    /**
     * Check for Swift reserved words.
     */
    static boolean isSwiftReservedWord(String ident)
    {
        return swiftReservedWords.contains(ident)
    }
    /**
     * Make sure identifier is a legal Swift name and modify it if necessary.
     */
    static String legalSwiftName(String identifier)
    {
        if (identifier==null || identifier.trim().length()==0)
            return identifier
        if (Character.isDigit(identifier.charAt(0))) {
            return '_'+identifier
        }
        return isSwiftReservedWord(identifier) ? identifier+'_' : identifier
    }
    /** legal Swift properties start with lower-case */
    static String legalSwiftPropertytName(String identifier)
    {
        return lowerCase(legalSwiftName(identifier))
    }

    static String camelBackSwiftClass(String anyString)
    {
        return legalSwiftName( upperCase(camelBackName(anyString)) )
    }

    /**
     * Generate a legal allUpperCase or starting lowercase enum name given an arbitrary string.
     */
    static String swiftEnumName(String anyString, boolean allUpperCase=false)
    {
        if (anyString==null || anyString.trim().length()==0)
            return null
        String normalized = replaceSpecialChars(anyString, ' ,_-&/', (char)'_')
        return allUpperCase ? legalSwiftName(normalized.toUpperCase()) : legalSwiftName(lowerCase(normalized))
    }

    /**
     * Convert arbitrary stirng to legal Swift constant name.  All non-legal
     * identifier characters are converted to '_'.
     */
    static String swiftConstName(String anyString)
    {
        if (anyString == null)
            return null
        StringBuilder swiftConst = new StringBuilder()
        int strlen = anyString.length()
        char lastChar = '\0'
        for(int i = 0; i < strlen; i++) {
            char c = Character.toUpperCase( anyString.charAt(i) )
            boolean validId = Character.isJavaIdentifierPart(c) && (c < 128) //close enough for now
            if (validId || lastChar != '_')
                swiftConst.append( validId ? c : '_')
            lastChar = validId ? c : (char)'_'
        }
        return legalSwiftName( swiftConst.toString() )
    }

}
