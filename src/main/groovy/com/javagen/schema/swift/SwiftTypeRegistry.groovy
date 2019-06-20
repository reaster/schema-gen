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

package com.javagen.schema.swift

import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry

/**
 * Create a Swift-specific instance of the type registry.
 *
 * usage: MTypeRegistry.instance().lookupType('String')
 *
 * @author Richard Easterling
 */
class SwiftTypeRegistry extends MTypeRegistry
{
    SwiftTypeRegistry(Map<String, MType> types)
    {
        super(types)
    }
    SwiftTypeRegistry()
    {
        this(defaultTypes())
    }

    static MType VOID = new MType(name:'Void')
    @Override MType getVOID() { VOID }
    @Override MType lookupTypeSpecial(String name)
    {
        null //throw new Error("method not implemented - type: ${name}")
    }
    @Override MType typeForCardinality(MCardinality cardinality)
    {
        String name = containerClassName(cardinality)
        types[name]
    }

    private static Map<String, MType> defaultTypes()
    {
        def t = []
        t << VOID
        t << new MType(name:'Any', val: 'nil')
        t << new MType(name:'String', val: '')
        t << new MType(name:'Int', val: '0')
        t << new MType(name:'Int8', val: '0')
        t << new MType(name:'Int16', val: '0')
        t << new MType(name:'Int32', val: '0')
        t << new MType(name:'Int64', val: '0')
        t << new MType(name:'UInt', val: '0')
        t << new MType(name:'UInt8', val: '0')
        t << new MType(name:'UInt16', val: '0')
        t << new MType(name:'UInt32', val: '0')
        t << new MType(name:'UInt64', val: '0')
        t << new MType(name:'Float', val: '0.0')
        t << new MType(name:'Double', val: '0.0')
        t << new MType(name:'Bool', val: 'false')
        t << new MType(name:'Character', val: '?')
        t << new MType(name:'UUID', val: 'UUID()')
//        t << new MType(name:'Integer', val: '0')
//        t << new MType(name:'Long', val: '0')
//        t << new MType(name:'Float', val: '0.0')
//        t << new MType(name:'Double', val: '0.0')
//        t << new MType(name:'Boolean', val: 'nil')
        t << new MType(name:'Date', val: 'Date()')
        t << new MType(name:'Optional')
        t << new MType(name:'Array', val: '[]')
        t << new MType(name:'Set', val: '[]')
        t << new MType(name:'Dictionary', val: '[:]')
        t << new MType(name:'Locale', val: 'Locale.current')
        //t << new MType(name:'java.time.ZonedDateTime')
        t << new MType(name:'URL')
        Map<String, MType> result = [:]
        for(MType type in t) {
            result[type.name] = type
        }
        result
    }

     static String containerClassName(MCardinality container)
    {
        switch (container) {
            case MCardinality.MAP: return 'Dictionary'
            case MCardinality.SET: return 'Set'
            case MCardinality.LIST: return 'Array'
            case MCardinality.ARRAY: return 'Array'
            case MCardinality.OPTIONAL: return 'Optional'
            default: return null
        }
    }

    public static Map<String,String> simpleXmlTypeToPropertyType = [
            'anyType':'Any',
            'anySimpleType':'Any',
            //https://www.w3.org/TR/xmlschema-2/#built-in-primitive-datatypes
            'string':'String',
            'double':'Double',
            'float':'Float',
            'decimal':'Double',
            'boolean':'Bool',
            'duration':'String', //TODO
            'dateTime':'Date',
            'time':'Date',
            'date':'Date',
            'gYearMonth':'String', //TODO
            'gYear':'Int',
            'gMonthDay':'String', //TODO
            'gDay':'UInt8', //1st,2nd,3rd,etc. day of the month
            'gMonth':'UInt8',
            'hexBinary':'Data',
            'base64Binary':'Data',
            'anyURI':'String',
            'QName':'String', //TODO
            'NOTATION':'String',
            //https://www.w3.org/TR/xmlschema-2/#built-in-derived
            'integer':'Int',
            'nonPositiveInteger':'Int32',
            'nonNegativeInteger':'Int32',
            'long':'Int64',
            'int':'Int32',
            'short':'UInt16',
            'byte':'UInt8',
            'negativeInteger':'Int32',
            'positiveInteger':'Int32',
            'unsignedLong':'Int64',
            'unsignedInt':'Int32',
            'unsignedShort':'UInt16',
            'unsignedByte':'UInt8',
            'normalizedString':'String',
            'token':'String',
            'language':'String',
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

}
