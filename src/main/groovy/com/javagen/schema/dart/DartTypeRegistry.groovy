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
class DartTypeRegistry extends MTypeRegistry
{
    DartTypeRegistry(Map<String, MType> types)
    {
        super(types)
    }

    DartTypeRegistry()
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

    private static Map<String, MType> defaultTypes()
    {
        def t = []
        t << VOID
        //CORE CLASSES
        t << new MType(name:'BidirectionalIterator')
        t << new MType(name:'BigInt')
        t << new MType(name:'bool', val: 'false')
        t << new MType(name:'DateTime', val: 'DateTime.now()')
        t << new MType(name:'Deprecated')
        t << new MType(name:'Comparable')
        t << new MType(name:'double', val: '0.0')
        t << new MType(name:'Duration')
        t << new MType(name:'dynamic')
        t << new MType(name:'Expando')
        t << new MType(name:'Function')
        t << new MType(name:'Future')
        t << new MType(name:'int', val: '0')
        t << new MType(name:'Invocation')
        t << new MType(name:'Iterable')
        t << new MType(name:'Iterator')
        t << new MType(name:'List', val: '[]')
        t << new MType(name:'Map', val: '{}')
        t << new MType(name:'MapEntry')
        t << new MType(name:'Match')
        t << new MType(name:'Null')
        t << new MType(name:'num')
        t << new MType(name:'Object')
        t << new MType(name:'Pattern')
        t << new MType(name:'pragma')
        t << new MType(name:'Provisional')
        t << new MType(name:'RegExp')
        t << new MType(name:'RuneIterator')
        t << new MType(name:'Runes')
        t << new MType(name:'Set')
        t << new MType(name:'Sink')
        t << new MType(name:'StackTrace')
        t << new MType(name:'Stream')
        t << new MType(name:'Stopwatch')
        t << new MType(name:'String', val: '')
        t << new MType(name:'StringBuffer', val: '')
        t << new MType(name:'StringSink')
        t << new MType(name:'Symbol')
        t << new MType(name:'Type')
        t << new MType(name:'Uri')
        t << new MType(name:'UriData')
        //CONSTANTS
        t << new MType(name:'deprecated')
        t << new MType(name:'override')
        t << new MType(name:'provisional')
        t << new MType(name:'proxy')
        //FUNCTIONS
        t << new MType(name:'identical')
        t << new MType(name:'identityHashCode')
        t << new MType(name:'print')
        //TYPEDEFS
        t << new MType(name:'Comparator')
        //EXCEPTIONS
        t << new MType(name:'AbstractClassInstantiationError')
        t << new MType(name:'ArgumentError')
        t << new MType(name:'AssertionError')
        t << new MType(name:'CastError')
        t << new MType(name:'ConcurrentModificationError')
        t << new MType(name:'CyclicInitializationError')
        t << new MType(name:'Error')
        t << new MType(name:'Exception')
        t << new MType(name:'FallThroughError')
        t << new MType(name:'FormatException')
        t << new MType(name:'IndexError')
        t << new MType(name:'IntegerDivisionByZeroException')
        t << new MType(name:'NoSuchMethodError')
        t << new MType(name:'NullThrownError')
        t << new MType(name:'OutOfMemoryError')
        t << new MType(name:'StackOverflowError')
        t << new MType(name:'StateError')
        t << new MType(name:'TypeError')
        t << new MType(name:'UnimplementedError')
        t << new MType(name:'UnsupportedError')
        //https://api.dartlang.org/stable/2.1.0/dart-async/dart-async-library.html
        //CLASSES (seleect few)
        t << new MType(name:'Future')
        t << new MType(name:'Stream')
        Map<String, MType> result = [:]
        for(MType type in t) {
            type.builtIn = true
            result[type.name] = type
        }
        result
    }











    public static Map<String,String> simpleXmlTypeToPropertyType = [
            'anyType':'dynamic',
            'anySimpleType':'dynamic', //dynamic ??
            //https://www.w3.org/TR/xmlschema-2/#built-in-primitive-datatypes
            'string':'String',
            'double':'double',
            'float':'double',
            'decimal':'double',
            'boolean':'bool',
            'duration':'Duration',
            'dateTime':'DateTime',
            'time':'DateTime',
            'date':'DateTime',
            'gYearMonth':'String', //TODO
            'gYear':'int',
            'gMonthDay':'String', //TODO
            'gDay':'int', //1st,2nd,3rd,etc. day of the month
            'gMonth':'int',
            'hexBinary':'ByteData',     //requires dart:typed_data library
            'base64Binary':'ByteData',  //requires dart:typed_data library
            'anyURI':'Uri',
            'QName':'String',
            'NOTATION':'String',
            //https://www.w3.org/TR/xmlschema-2/#built-in-derived
            'integer':'int',
            'nonPositiveInteger':'int',
            'nonNegativeInteger':'int',
            'long':'int',
            'int':'int',
            'short':'int',
            'byte':'int',
            'negativeInteger':'int',
            'positiveInteger':'int',
            'unsignedLong':'int',
            'unsignedInt':'int',
            'unsignedShort':'int',
            'unsignedByte':'int',
            'normalizedString':'String',
            'token':'String',
            'language':'Locale', //requires locale.dart library
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
        switch (cardinality) {
            case MCardinality.LINKEDMAP: return 'LinkedHashMap'
            case MCardinality.MAP: return 'Map'
            case MCardinality.SET: return 'Set'
            case MCardinality.LIST: return 'List'
            case MCardinality.ARRAY: return 'List'
        //case MCardinality.OPTIONAL: return 'java.util.Optional'
            default: return null
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


    static Set<String> floatingPointTypeSet = ['double']

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
