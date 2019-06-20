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

package com.javagen.schema.kotlin

import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry


/**
 * Create a Kotlin-specific instance of the type registry.
 *
 * usage: MTypeRegistry.instance().lookupType('String')
 *
 * @author Richard Easterling
 */
class KotlinTypeRegistry extends MTypeRegistry
{
    KotlinTypeRegistry(Map<String, MType> types)
    {
        super(types)
    }

    KotlinTypeRegistry()
    {
        this(defaultTypes())
        //java.time.LocalTime.now()
    }

    static MType VOID = new MType(name:'void',primitive:true)
    @Override MType getVOID() { VOID }
    @Override MType lookupTypeSpecial(String name)
    {
        null //throw new Error("method not implemented - type: ${name}")
    }
    @Override MType typeForCardinality(MCardinality cardinality)
    {
        String name = container(cardinality)
        types[name]
    }

    private static Map<String, MType> defaultTypes()
    {
        def t = []
        t << VOID
        t << new MType(name:'String', val: '')
        t << new MType(name:'java.util.Date', val: 'java.util.Date()')
        t << new MType(name:'java.util.Optional', val: '')
        t << new MType(name:'Set', val: 'setOf()')
        t << new MType(name:'List', val: 'listOf()')
        t << new MType(name:'Map', val: 'mapOf()')
        t << new MType(name:'MutableSet', val: 'mutableSetOf()')
        t << new MType(name:'MutableList', val: 'mutableListOf()')
        t << new MType(name:'MutableMap', val: 'mutableMapOf()')
        t << new MType(name:'Array', val: 'emptyArray()')
        //TODO add ByteArray, ShortArray, IntArray, CharArray, DoubleArray, FloatArray
        t << new MType(name:'java.util.Locale', val: 'java.util.Locale.getDefault()')
        t << new MType(name:'Char', val: '\0')
        t << new MType(name:'Byte', val: '0')
        t << new MType(name:'Short', val: '0')
        t << new MType(name:'Int', val: '0')
        t << new MType(name:'Long', val: '0L')
        t << new MType(name:'Float', val: '0.0F')
        t << new MType(name:'Double', val: '0.0')
        t << new MType(name:'Boolean', val: 'false')
        t << new MType(name:'java.math.BigDecimal', val: 'java.math.BigDecimal(0)')
        t << new MType(name:'java.time.LocalTime', val: 'java.time.LocalTime.now()')
        t << new MType(name:'java.time.LocalDateTime', val: 'java.time.LocalDateTime.now()')
        t << new MType(name:'java.time.LocalDate', val: 'java.time.LocalDate.now()')
        t << new MType(name:'java.time.ZonedDateTime', val: 'java.time.ZonedDateTime.now()')
        t << new MType(name:'java.net.URL')
        t << new MType(name:'Any')
        Map<String, MType> result = [:]
        for(MType type in t) {
            type.builtIn = true
            result[type.name] = type
        }
        result
    }

    public static Map<String,String> simpleXmlTypeToPropertyType = [
            'anyType':'Any',
            'anySimpleType':'Any',
            //https://www.w3.org/TR/xmlschema-2/#built-in-primitive-datatypes
            'string':'String',
            'double':'Double',
            'float':'Float',
            'decimal':'Double',
            'boolean':'Boolean',
            'duration':'String', //TODO
            'dateTime':'java.time.LocalDateTime',
            'time':'java.time.LocalTime',
            'date':'java.time.LocalDate',
            'gYearMonth':'String', //TODO
            'gYear':'Int',
            'gMonthDay':'String', //TODO
            'gDay':'Byte', //1st,2nd,3rd,etc. day of the month
            'gMonth':'Byte',
            'hexBinary':'Byte[]',
            'base64Binary':'Byte[]',
            'anyURI':'String',
            'QName':'String', //TODO
            'NOTATION':'String',
            //https://www.w3.org/TR/xmlschema-2/#built-in-derived
            'integer':'Int',
            'nonPositiveInteger':'Int',
            'nonNegativeInteger':'Int',
            'long':'Long',
            'int':'Int',
            'short':'Short',
            'byte':'Byte',
            'negativeInteger':'Int',
            'positiveInteger':'Int',
            'unsignedLong':'Long',
            'unsignedInt':'Int',
            'unsignedShort':'Short',
            'unsignedByte':'Byte',
            'normalizedString':'String',
            'token':'String',
            'language':'java.util.Locale',
            'Name':'String',
            'NMTOKEN':'String',
            'NMTOKENS':'String',
            'NCName':'String',
            'ID':'String',
            'IDREF':'String',
            'IDREFS':'String',
            'ENTITY':'String',
            'ENTITIES':'String'
    ]

    static boolean isBoolean(MType type)
    {
        type.name.equals('Boolean')
    }

    static String container(MCardinality cardinality, boolean mutable=true)
    {
        if (mutable) {
            switch (cardinality) {
                case MCardinality.LINKEDMAP: return 'LinkedHashMap'
                case MCardinality.MAP: return 'MutableMap'
                case MCardinality.SET: return 'MutableSet'
                case MCardinality.LIST: return 'MutableList'
                case MCardinality.ARRAY: return 'Array'
            //case MCardinality.OPTIONAL: return 'java.util.Optional'
                default: return null
            }
        } else {
            switch (cardinality) {
                case MCardinality.LINKEDMAP: return 'LinkedHashMap'
                case MCardinality.MAP: return 'Map'
                case MCardinality.SET: return 'Set'
                case MCardinality.LIST: return 'List'
                case MCardinality.ARRAY: return 'Array'
            //case MCardinality.OPTIONAL: return 'java.util.Optional'
                default: return null
            }
        }
    }
    static String containerImplementation(MCardinality cardinality)
    {
    }

    /* this Java-specific logic is not needed */
    static String useWrapper(String type)
    {
        false
    }


    static Set<String> floatingPointTypeSet = ['float','Float','double','Double']

    static boolean isFloatingPointType(String type)
    {
        floatingPointTypeSet.contains(type)
    }

    static EnumSet<MCardinality> containerRequiresWrapper = EnumSet.of(MCardinality.SET,MCardinality.LIST,MCardinality.OPTIONAL)

    static boolean containerRequiresPrimitiveWrapper(MCardinality cardinality)
    {
        containerRequiresWrapper.contains(cardinality)
    }
}
