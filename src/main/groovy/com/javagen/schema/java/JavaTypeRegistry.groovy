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

package com.javagen.schema.java

import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry


/**
 * Create a Java-specific instance of the type registry.
 *
 * usage: MTypeRegistry.instance().lookupType('String')
 */
class JavaTypeRegistry extends MTypeRegistry
{
    JavaTypeRegistry(Map<String, MType> types)
    {
        super(types)
    }
    JavaTypeRegistry()
    {
        this(defaultTypes())
        //java.time.LocalTime.now()
    }

    static MType VOID = new MType(name:'void',primitive:true)
    MType getVOID() { VOID }

    private static Map<String, MType> defaultTypes()
    {
        def t = []
        t << VOID
        t << new MType(name:'String', val: '')
        t << new MType(name:'java.common.Date', val: 'new java.common.Date()')
        t << new MType(name:'java.common.Optional', val: 'java.common.Optional.empty()')
        t << new MType(name:'java.common.Map', val: 'new java.common.HashMap<>()')
        t << new MType(name:'java.common.Set', val: 'new java.common.HashSet<>()')
        t << new MType(name:'java.common.List', val: 'new java.common.ArrayList<>()')
        t << new MType(name:'java.common.HashMap', val: 'new java.common.HashMap<>()')
        t << new MType(name:'java.common.HashSet', val: 'new java.common.HashSet<>()')
        t << new MType(name:'java.common.ArrayList', val: 'new java.common.ArrayList<>()')
        t << new MType(name:'java.common.Locale', val: 'java.common.Locale.getDefault()')
        t << new MType(name:'char',primitive:true, val: '\0')
        t << new MType(name:'byte',primitive:true, val: '0')
        t << new MType(name:'short',primitive:true, val: '0')
        t << new MType(name:'int',primitive:true, val: '0')
        t << new MType(name:'long',primitive:true, val: '0L')
        t << new MType(name:'float',primitive:true, val: '0.0F')
        t << new MType(name:'double',primitive:true, val: '0.0')
        t << new MType(name:'boolean',primitive:true, val: 'false')
        t << new MType(name:'Charactar', val: '\0')
        t << new MType(name:'Byte', val: '0')
        t << new MType(name:'Short', val: '0')
        t << new MType(name:'Integer', val: '0')
        t << new MType(name:'Long', val: '0L')
        t << new MType(name:'Float', val: '0.0F')
        t << new MType(name:'Double', val: '0.0')
        t << new MType(name:'Boolean', val: 'Boolean.FALSE')
        t << new MType(name:'java.common.BigDecimal', val: 'new java.common.BigDecimal(0)')
        t << new MType(name:'java.time.LocalTime', val: 'java.time.LocalTime.now()')
        t << new MType(name:'java.time.LocalDateTime', val: 'java.time.LocalDateTime.now()')
        t << new MType(name:'java.time.LocalDate', val: 'java.time.LocalDate.now()')
        t << new MType(name:'java.time.ZonedDateTime', val: 'java.time.ZonedDateTime.now()')
        t << new MType(name:'java.net.URL')
        t << new MType(name:'Object')
        Map<String, MType> result = [:]
        for(MType type in t) {
            type.builtIn = true
            result[type.name] = type
        }
        result
    }

    static Map<String,String> simpleXmlTypeToPropertyType = [
            'anyType':'Object',
            'anySimpleType':'Object',
            //https://www.w3.org/TR/xmlschema-2/#built-in-primitive-datatypes
            'string':'String',
            'decimal':'double',
            'boolean':'boolean',
            'duration':'String', //TODO
            'dateTime':'java.time.LocalDateTime',
            'time':'java.time.LocalTime',
            'date':'java.time.LocalDate',
            'gYearMonth':'String', //TODO
            'gYear':'int',
            'gMonthDay':'String', //TODO
            'gDay':'byte', //1st,2nd,3rd,etc. day of the month
            'gMonth':'byte',
            'hexBinary':'byte[]',
            'base64Binary':'byte[]',
            'anyURI':'String',
            'QName':'String', //TODO
            'NOTATION':'String',
            //https://www.w3.org/TR/xmlschema-2/#built-in-derived
            'integer':'int',
            'nonPositiveInteger':'int',
            'nonNegativeInteger':'int',
            'long':'long',
            'int':'int',
            'short':'short',
            'byte':'byte',
            'negativeInteger':'int',
            'positiveInteger':'int',
            'unsignedLong':'long',
            'unsignedInt':'int',
            'unsignedShort':'short',
            'unsignedByte':'byte',
            'normalizedString':'String',
            'token':'String',
            'language':'java.common.Locale',
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
        type.name.equals('boolean') || type.name.equals('Boolean')
    }

    static String containerInterface(MCardinality cardinality)
    {
        switch (cardinality) {
            case MCardinality.MAP: return 'java.common.Map'
            case MCardinality.SET: return 'java.common.Set'
            case MCardinality.LIST: return 'java.common.List'
            case MCardinality.OPTIONAL: return 'java.common.Optional'
            default: return null
        }
    }
    static String containerImplementation(MCardinality cardinality)
    {
        switch (cardinality) {
            case MCardinality.MAP: return 'java.common.HashMap'
            case MCardinality.SET: return 'java.common.HashSet'
            case MCardinality.LIST: return 'java.common.ArrayList'
            case MCardinality.OPTIONAL: return 'java.common.Optional'
            default: return null
        }
    }

    static String useWrapper(String type)
    {
        if (!type)
            return type
        final String wrapper = primitiveToWrapperMap[type]
        wrapper ?: type
    }

    static Map<String,String> primitiveToWrapperMap = [
            'boolean':'Boolean',
            'char':'Character',
            'short':'Short',
            'byte':'Byte',
            'int':'Integer',
            'long':'Long',
            'float':'Float',
            'double':'Double'
    ]

    static Set<String> floatingPointTypeSet = ['float','Float','double','Double']

    static boolean isFloatingPointType(String type)
    {
        floatingPointTypeSet.contains(type)
    }

    static Map<String,String> wrapperToPrimitiveMap = primitiveToWrapperMap.collectEntries { k,v -> [v:k] }

    static boolean isWrapper(String type)
    {
        type ? wrapperToPrimitiveMap[type] : false
    }
    static boolean isPrimitive(String type)
    {
        type ? primitiveToWrapperMap[type] : false
    }

    static EnumSet<MCardinality> containerRequiresWrapper = EnumSet.of(MCardinality.SET,MCardinality.LIST,MCardinality.OPTIONAL)

    boolean containerRequiresPrimitiveWrapper(MCardinality cardinality)
    {
        containerRequiresWrapper.contains(cardinality)
    }
}
