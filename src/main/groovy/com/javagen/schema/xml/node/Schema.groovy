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

package com.javagen.schema.xml.node

import com.javagen.schema.xml.QName

import static com.javagen.schema.common.GlobalFunctionsUtil.extractNamespacePrefix
import static com.javagen.schema.common.GlobalFunctionsUtil.stripNamespace

//@ToString(includeNames=true,includePackage=false,excludes='TARGET_PREFIX,DEFAULT_NS,DEFAULT_PREFIX')
class Schema implements ElementHolder, AttributeHolder
{
    static final String targetNamespace = 'targetNamespace'
    static final String DEFAULT_NS = 'http://www.w3.org/2001/XMLSchema'
    static final String DEFAULT_PREFIX = 'xsd'
    Map<String,String> prefixToNamespaceMap = [:]
    java.util.List<QName> rootElements = []

    Map<QName,AttributeGroup> globalAttributeGroups = [:]
    Map<QName,Group> globalGroups = [:]
    Map<QName,Type> globalTypes = [:] //combines TextOnlyType, SimpleType and ComplextType

    QName qname(String nameWithPrefix)
    {
        if (!nameWithPrefix)
            return null
        String prefix = extractNamespacePrefix(nameWithPrefix)
        String namespace = prefixToNamespaceMap[prefix]
        if (!namespace)
            namespace = prefixToNamespaceMap[targetNamespace]
        if (!namespace)
            throw new IllegalStateException("no namespace registered for prefix '${prefix}' for name: ${nameWithPrefix}")
        new QName(namespace:namespace,name: stripNamespace(nameWithPrefix))
    }

    def putGlobal(String namespace, String name, Type node)
    {
        putGlobal(new QName(namespace:namespace,name:name), node)
    }
    def putGlobal(String nameWithPrefix, Type node)
    {
        putGlobal(qname(nameWithPrefix), node)
    }
    def putGlobal(QName qname, Type node)
    {
        globalTypes[qname] = node
    }
    def putGlobal(Type node)
    {
        if (!node)
            return
        if (!node.qname?.name)
            throw new IllegalStateException("can't add a type without a qname: ${node}")
        globalTypes[node.qname] = node
    }
    def getGlobal(String namespace, String name)
    {
        (namespace && name) ? getGlobal(new QName(namespace:namespace,name:name)) : null
    }
    def getGlobal(QName qname)
    {
        qname ? globalTypes[qname] : null
    }
    def getGlobal(String nameWithPrefix)
    {
        nameWithPrefix ? getGlobal(qname(nameWithPrefix)) : null
    }

    static final def BUILT_IN_TYPES = [
            'anyType:',
            'anySimpleType:anyType',
            //https://www.w3.org/TR/xmlschema-2/#built-in-primitive-datatypes
            'string:anySimpleType',
            'decimal:anySimpleType',
            'boolean:anySimpleType',
            'float:anySimpleType',
            'double:anySimpleType',
            'duration:anySimpleType',
            'dateTime:anySimpleType',
            'time:anySimpleType',
            'date:anySimpleType',
            'gYearMonth:anySimpleType',
            'gYear:anySimpleType',
            'gMonthDay:anySimpleType',
            'gDay:anySimpleType',
            'gMonth:anySimpleType',
            'hexBinary:anySimpleType',
            'base64Binary:anySimpleType',
            'anyURI:anySimpleType',
            'QName:anySimpleType',
            'NOTATION:anySimpleType',
            //https://www.w3.org/TR/xmlschema-2/#built-in-derived
            'integer:decimal',
            'nonPositiveInteger:integer',
            'nonNegativeInteger:integer',
            'long:integer',
            'int:long',
            'short:int',
            'byte:short',
            'negativeInteger:nonPositiveInteger',
            'positiveInteger:nonNegativeInteger',
            'unsignedLong:nonNegativeInteger',
            'unsignedInt:unsignedLong',
            'unsignedShort:unsignedInt',
            'unsignedByte:unsignedShort',
            'normalizedString:stirng',
            'token:normalizedString',
            'language:token',
            'Name:token',
            'NMTOKEN:token',
            'NMTOKENS:NMTOKEN',
            'NCName:Name',
            'ID:NCName',
            'IDREF:NCName',
            'IDREFS:IDREF',
            'ENTITY:NCName',
            'ENTITIES:ENTITY'
    ]

    def initTypes()
    {
        for(nameSuperPair in BUILT_IN_TYPES) {
            def pair = nameSuperPair.split(':')
            QName qName = new QName(name:pair[0])
            Type superType = pair.length > 1 ?globalTypes[new QName(name:pair[1])] : null
            TextOnlyType type = new TextOnlyType(qname:qName,base:superType,builtInType:true)
            globalTypes[qName] = type
        }
    }

    Schema()
    {
        initTypes()
    }

    @Override String toString()
    {
        def s = 'Schema[\n'
        s+= '  prefixToNamespaceMap[\n'
        for(def e : prefixToNamespaceMap)
            s+= "    ${e.key} -> ${e.value}\n"
        s+= '  ]\n'
        s+= '  globalAttributeGroups[\n'
        for(def e : globalAttributeGroups)
            s+= "    ${e.key} -> ${e.value}\n"
        s+= '  ]\n'
        s+= '  globalGroups[\n'
        for(def e : globalGroups)
            s+= "    ${e.key} -> ${e.value}\n"
        s+= '  ]\n'
        s+= '  globalTypes[\n'
        for(def e : globalTypes)
            s+= "    ${e.key} -> ${e.value}\n"
        s+= '  ]\n'
        s+= ']\n'
        s
    }
}
