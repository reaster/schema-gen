package com.javagen.schema.kotlin

import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry


/**
 * Create a Kotlin-specific instance of the type registry.
 *
 * usage: MTypeRegistry.instance().lookupType('String')
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
    MType getVOID() { VOID }

    private static Map<String, MType> defaultTypes()
    {
        def t = []
        t << VOID
        t << new MType(name:'String', val: '')
        t << new MType(name:'java.common.Date', val: 'java.common.Date()')
        t << new MType(name:'java.common.Optional', val: '')
        t << new MType(name:'Set', val: 'setOf()')
        t << new MType(name:'List', val: 'listOf()')
        t << new MType(name:'Map', val: 'mapOf()')
        t << new MType(name:'MutableSet', val: 'mutableSetOf()')
        t << new MType(name:'MutableList', val: 'mutableListOf()')
        t << new MType(name:'MutableMap', val: 'mutableMapOf()')
        t << new MType(name:'Array', val: 'emptyArray()')
        //TODO add ByteArray, ShortArray, IntArray, CharArray, DoubleArray, FloatArray
        t << new MType(name:'java.common.Locale', val: 'java.common.Locale.getDefault()')
        t << new MType(name:'Char', val: '\0')
        t << new MType(name:'Byte', val: '0')
        t << new MType(name:'Short', val: '0')
        t << new MType(name:'Int', val: '0')
        t << new MType(name:'Long', val: '0L')
        t << new MType(name:'Float', val: '0.0F')
        t << new MType(name:'Double', val: '0.0')
        t << new MType(name:'Boolean', val: 'false')
        t << new MType(name:'java.common.BigDecimal', val: 'java.common.BigDecimal(0)')
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
            //case MCardinality.OPTIONAL: return 'java.common.Optional'
                default: return null
            }
        } else {
            switch (cardinality) {
                case MCardinality.LINKEDMAP: return 'LinkedHashMap'
                case MCardinality.MAP: return 'Map'
                case MCardinality.SET: return 'Set'
                case MCardinality.LIST: return 'List'
                case MCardinality.ARRAY: return 'Array'
            //case MCardinality.OPTIONAL: return 'java.common.Optional'
                default: return null
            }
        }
    }
    static String containerImplementation(MCardinality cardinality)
    {
    }

    static String useWrapper(String type)
    {
        false
//        if (!type)
//            return type
//        final String wrapper = primitiveToWrapperMap[type]
//        wrapper ?: type
    }

//    static Map<String,String> primitiveToWrapperMap = [
//            'boolean':'Boolean',
//            'char':'Character',
//            'short':'Short',
//            'byte':'Byte',
//            'int':'Integer',
//            'long':'Long',
//            'float':'Float',
//            'double':'Double'
//    ]

    static Set<String> floatingPointTypeSet = ['float','Float','double','Double']

    static boolean isFloatingPointType(String type)
    {
        floatingPointTypeSet.contains(type)
    }

//    static Map<String,String> wrapperToPrimitiveMap = primitiveToWrapperMap.collectEntries { k,v -> [v:k] }

//    static boolean isWrapper(String type)
//    {
//        type ? wrapperToPrimitiveMap[type] : false
//    }
//    static boolean isPrimitive(String type)
//    {
//        type ? primitiveToWrapperMap[type] : false
//    }

    static EnumSet<MCardinality> containerRequiresWrapper = EnumSet.of(MCardinality.SET,MCardinality.LIST,MCardinality.OPTIONAL)

    static boolean containerRequiresPrimitiveWrapper(MCardinality cardinality)
    {
        containerRequiresWrapper.contains(cardinality)
    }
}
