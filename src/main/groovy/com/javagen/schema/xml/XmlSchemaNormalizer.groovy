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

package com.javagen.schema.xml

import com.javagen.schema.xml.node.Any
import com.javagen.schema.xml.node.AnyAttribute
import com.javagen.schema.xml.node.Attribute
import com.javagen.schema.xml.node.AttributeGroup
import com.javagen.schema.xml.node.AttributeHolder
import com.javagen.schema.xml.node.ComplexType
import com.javagen.schema.xml.node.Element
import com.javagen.schema.xml.node.ElementHolder
import com.javagen.schema.xml.node.Group
import com.javagen.schema.xml.node.Restriction
import com.javagen.schema.xml.node.Schema
import com.javagen.schema.xml.node.SimpleType
import com.javagen.schema.xml.node.TextOnlyType
import com.javagen.schema.xml.node.Type
import com.javagen.schema.xml.node.Union
import com.javagen.schema.xml.node.Value
import groovy.util.slurpersupport.GPathResult

import static com.javagen.schema.common.GlobalFunctionsUtil.*

/**
 * Generate a simplified XML Schema object model. Normalization includes removing references,
 * removing nested definitions, expanding attributeGroup and element Group nodes and mapping compound types to a
 * more expressive model: TextOnlyType, SimpleType and ComplexType.
 *
 * <p>Although it complicates the object model, all
 * nodes are referenced using QName instances with the proper namespace.
 *
 * @author Richard Easterling
 */
class XmlSchemaNormalizer
{
    Schema schema
    Stack<ElementHolder> elementStack = new Stack<>()
    Stack<AttributeHolder> attributeStack = new Stack<>()
    Stack<Value> valueStack = new Stack<>()
    Stack<TextOnlyType> simpleTypeStack = new Stack<>()
    boolean verbose = false
    boolean inlineGroups = true

    Stack<Map<String,String>> prefixToNamespaceMap = new Stack<>()

    Map<QName,Object> simpleTypeNodeLookup = [:]
    Map<QName,Object> groupNodeLookup = [:]
    Map<QName,Object> attributeGroupNodeLookup = [:]
    Map<QName,Object> complexTypeNodeLookup = [:]
    Map<QName,Object> globalAttributeNodeLookup = [:]
    Map<QName,Object> globalElementNodeLookup = [:]

    int indent = 0
    def tab = '  '

    /**
     * Recursively walk XML Schema generating a simplified Schema model. Model objects are placed on a set of stacks
     * allowing child object (attributes, elements and types) to be assigned in their proper owners. Global nodes
     * should be pre-indexed (by calling indexGlobalNodes) so @ref attributes can be resolved before the
     * target node has been processed.
     *
     * @param node any
     * @return
     */
    def traverseElements(def node)
    {
        if (indent > 30) {
            println 'overflow...'
            return
        }

        for (child in node.'*') {
            String tag = child.name()
            if (!tag)
                throw new IllegalStateException("no element tag found for ${node}")
            String name = child.@name?.text()
            String ref = child.@ref?.text()
            String id = child.@ID?.text()
            String typeWithPrefix = child.@type?.text()
            Type type = typeWithPrefix ? schema.getGlobal(qname(typeWithPrefix)) : null
            if (typeWithPrefix && !type) {
                throw new IllegalStateException("${tag} @name='${name}' has @type='${typeWithPrefix}' but NO global type was found")
            }
            switch (tag) {
                case 'simpleType':
                    name = name ?: node.@name?.text()
                    if (!name)
                        throw new IllegalStateException("simpleType must have name defined")
                    if (isTextOnlyType(child)) {
                        TextOnlyType textOnlyType = schema.getGlobal(qname(name)) ?: new TextOnlyType(qname:name,base:type,id:id)
                        simpleTypeStack.push(textOnlyType)
                        traverseElements(child)
                        if (child.@name?.text()) {
                            schema.putGlobal(textOnlyType)
                        } else {
                            valueStack.peek().type = textOnlyType
                        }
                        simpleTypeStack.pop()
                    } else {
                        SimpleType simpleType = schema.getGlobal(qname(name)) ?: new SimpleType(qname:name,base:type,id:id)
                        simpleTypeStack.push(simpleType)
                        attributeStack.push(simpleType)
                        traverseElements(child)
                        if (child.@name?.text()) {
                            schema.putGlobal(simpleType)
                        } else {
                            valueStack.peek().type = simpleType
                        }
                        attributeStack.pop()
                        simpleTypeStack.pop()
                    }
                    break
                case 'complexType':
                    boolean isSimpleContent = child.simpleContent?.list()
                    if (isSimpleContent) {
                        traverseElements(child)
                    } else {
                        name = name ?: node.@name?.text()
                        boolean mixed = child.@mixed?.text() == 'true'
                        if (!name)
                            throw new IllegalStateException("complexType must have name defined")
                        ComplexType complexType = schema.getGlobal(qname(name)) ?: new ComplexType(qname:name,base:type,id:id)
                        complexType.mixedContent = mixed
                        elementStack.push(complexType)
                        attributeStack.push(complexType)
                        traverseElements(child)
                        if (child.@name?.text()) {
                            schema.putGlobal(complexType) //global
                        } else {
                            valueStack.peek().type = complexType //attributer or element
                        }
                        elementStack.pop()
                        attributeStack.pop()
                    }
                    break
                case 'complexContent':
                    traverseElements(child)
                    break
                case 'simpleContent':
                    name = name ?: node.@name?.text() ?: node.parent().@name?.text()
                    if (!name)
                        throw new IllegalStateException("simpleType must have name defined")
                    //boolean mixed = node.@mixed?.text() == 'true'
                    SimpleType simpleType = schema.getGlobal(qname(name)) ?: new SimpleType(qname:name,base:type,id:id)
                    simpleTypeStack.push(simpleType)
                    attributeStack.push(simpleType)
                    traverseElements(child)
                    if (node.@name?.text()) {
                        schema.putGlobal(simpleType) //global
                    } else {
                        valueStack.peek().type = simpleType //attribute or element
                    }
                    simpleTypeStack.pop()
                    attributeStack.pop()
                    break
                case 'element':
                    Element element = new Element(id:id)
                    def refNode
                    if (ref) {
                        refNode = globalElementNodeLookup[qname(ref)]
                        setElementProperties(element, refNode)
                    }
                    setElementProperties(element, child)
                    if (verbose) println "${tab*indent}<element name='${name}' type='${type}' ref='${ref}'>"
                    valueStack.push(element)
                    indent+=1
                    traverseElements(child)
                    elementStack.peek().elements << element
                    valueStack.pop()
                    indent-=1
                    break
                case 'any':
                    Any any = new Any(id:id)
                    setAnyProperties(any, child)
                    if (verbose) println "${tab*indent}<any name='${name}' type='${type}' ref='${ref}'>"
                    valueStack.push(any)
                    indent+=1
                    traverseElements(child)
                    elementStack.peek().elements << any
                    valueStack.pop()
                    indent-=1
                    break
                case 'attribute':
                    Attribute attribute = new Attribute(id:id)
                    def refNode
                    if (ref) {
                        QName attrQName = qname(ref)
                        refNode = globalAttributeNodeLookup[attrQName]
                        setAttributeProperties(attribute, refNode)
                    }
                    setAttributeProperties(attribute, child)
                    if (verbose) println "${tab*indent}<attribute name='${name}' type='${type}' ref='${ref}'>"
                    valueStack.push(attribute)
                    traverseElements(child)
                    AttributeHolder attributeHolder = attributeStack.peek()
                    if (attributeHolder) {
                        attributeHolder.attributes << attribute
                    } else {
                        println "no AttributeHolder found on attributeStack, ignoring xml attriubte: ${attribute}"
                    }
                    valueStack.pop()
                    break
                case 'anyAttribute':
                    Attribute attribute = new AnyAttribute(qname:name)
                    valueStack.push(attribute)
                    traverseElements(child)
                    AttributeHolder attributeHolder = attributeStack.peek()
                    if (attributeHolder) {
                        attributeHolder.attributes << attribute
                    } else {
                        println "no AttributeHolder found on attributeStack, ignoring anyAttribute: ${attribute}"
                    }
                    valueStack.pop()
                    break
                case 'attributeGroup':
                    if (ref) {
                        def attributeGroup = attributeGroupNodeLookup[qname(ref)]
                        if (inlineGroups) {
                            traverseElements(attributeGroup) //just add them to the current type
                        } else {
                            attributeStack.peek().attributeGroups << attributeGroup
                        }
                    } else if (name) {
                        AttributeGroup attrGroup = schema.globalAttributeGroups[qname(name)] ?: new AttributeGroup(qname:name)
                        attributeStack.push(attrGroup)
                        traverseElements(child)
                        schema.globalAttributeGroups.put(attrGroup.qname, attrGroup)
                        attributeStack.pop()
                    } else {
                        throw new Error("nested attributeGroup not handled")
                    }
                    break
                case 'group':
                    if (ref) {
                        def group = groupNodeLookup[qname(ref)]
                        if (inlineGroups) {
                            traverseElements(group) //just add them to the current type
                        } else {
                            elementStack.peek().groups << group
                        }
                    } else if (name) {
                        Group group = schema.globalGroups[qname(name)] ?: new Group(qname:name)
                        elementStack.push(group)
                        traverseElements(child)
                        schema.globalGroups.put(group.qname, group)
                        elementStack.pop()
                    } else {
                        throw new Error("nested group not handled")
                    }
                    break
                case 'list':
                    String itemType = child.@itemType?.text()
                    def base = itemType ? schema.getGlobal( qname(itemType) ) : null
                    com.javagen.schema.xml.node.List list = new com.javagen.schema.xml.node.List(itemType:base)
                    simpleTypeStack.peek().base = list
                    break
                case 'union':
                    //<xs:union memberTypes="sizebynumber sizebystring" />
                    Union union = new Union()
                    String memberTypes = child.@memberTypes?.text()
                    if (memberTypes) {
                        for(String member : memberTypes.split(' ')) {
                            def simpleType = schema.getGlobal( qname(member) )
                            if (!simpleType)
                                throw new IllegalStateException("can not find type for union member: ${member}")
                            union.simpleTypes << simpleType
                        }
                    }
                    traverseElements(child) //nested simplTypes not captured yet...
                    simpleTypeStack.peek().base = union
                    break
                case 'sequence':
                    traverseElements(child)
                    break
                case 'choice':
                    traverseElements(child)
                    break
//                case 'any':
//                    traverseElements(child)
//                    break
                case 'notation':
                case 'unique':
                case 'selector':
                case 'redefine':
                case 'import':
                case 'include':
                case 'key':
                case 'keyref':
                case 'field':
                case 'annotation':
                case 'documentation':
                case 'appinfo':
                    break //TODO
                case 'enumeration':
                case 'minInclusive':
                case 'maxInclusive':
                case 'minExclusive':
                case 'maxExclusive':
                case 'pattern':
                case 'whiteSpace':
                case 'length':
                case 'minLength':
                case 'maxLength':
                case 'fractionDigits':
                case 'totalDigits':
                    String value = child.@value?.text()
                    simpleTypeStack.peek().restrictions << new Restriction(tag,value)
                    break
                case 'restriction':
                case 'extension':
                    //<element name="PremiseNumberPrefix"><complexType><simpleContent><extension base="xs:string"><attribute...
                    String base = child.@base?.text()
                    def baseType = base ? schema.getGlobal( qname(base) ) : null
                    if (baseType) {
                        Type parentType = findParentType(child)
                        parentType.base = baseType
                    }
                    traverseElements(child)
                    break
                default:
                    println "TODO add support to traverseElements() for node type: ${tag}"
                    throw new IllegalStateException("TODO add support to gatherElements() for node type: ${tag}")
            }
        }
    }

    QName qname(String nameWithPrefix)
    {
        if (!nameWithPrefix)
            return null
        String prefix = extractNamespacePrefix(nameWithPrefix)
        String namespace = prefixToNamespaceMap.peek()[prefix]
        if (!namespace)
            namespace = prefixToNamespaceMap.peek()[Schema.targetNamespace]
        if (!namespace)
            throw new IllegalStateException("no namespace registered for prefix '${prefix}' for name: ${nameWithPrefix}")
        new QName(namespace:namespace,name: stripNamespace(nameWithPrefix))
    }

    /**
     * index global nodes for easy lookup
     */
    def indexGlobalNodes(def schemaNode)
    {
        for(node in schemaNode.simpleType.list()) {
            QName qname = qname(node.@name.text())
            simpleTypeNodeLookup[qname] = node
            schema.putGlobal(qname, isTextOnlyType(node) ? new TextOnlyType(qname:qname) : new SimpleType(qname:qname))
        }
        for(node in schemaNode.complexType.list()) {
            QName qname = qname(node.@name.text())
            complexTypeNodeLookup[qname] = node
            boolean isSimpleContent = !node.simpleContent.list().isEmpty()
            schema.putGlobal(qname, isSimpleContent ? new SimpleType(qname:qname) : new ComplexType(qname:qname))
        }
        for(node in schemaNode.attributeGroup.list()) {
            QName qname = qname(node.@name.text())
            attributeGroupNodeLookup[qname] = node
            schema.globalAttributeGroups[qname] = new AttributeGroup(qname:qname)
        }
        for(node in schemaNode.group.list()) {
            QName qname = qname(node.@name.text())
            groupNodeLookup[qname] = node
            schema.globalGroups[qname] = new Group(qname:qname)
        }
        for(node in schemaNode.attribute.list()) {
            String name = node.@name.text()
            if (name.endsWith('lang'))
                println 'lang'
            QName qname = qname(name)
            globalAttributeNodeLookup[qname] = node
        }
        for(node in schemaNode.element.list()) {
            QName qname = qname(node.@name.text())
            globalElementNodeLookup[qname] = node
            if (qname.name == 'xAL')
                println qname
            generateGlobalTypeForElementIfEmbeddedType(qname, node)
        }
    }

    Type generateGlobalTypeForElementIfEmbeddedType(QName qname, def node)
    {
        Type type = null
        if ( ! node.@type?.text() ) {
            if (node.complexType?.simpleContent?.list()) {
                type = new SimpleType(qname:qname)
            } else if (node.complexType?.list()) {
                type = new ComplexType(qname:qname)
            } else if (node.simpleType?.list()) {
                type = new TextOnlyType(qname:qname)
            }
            if (type) {
                Type existingType = schema.getGlobal(qname)
                if (existingType) {
                    throw new IllegalStateException("can't create ${type.class.simpleName} for ${qname}, type with that name already exists: ${existingType}")
                } else {
                    if (verbose) println "global[${qname.name}] = ${type}"
                    schema.putGlobal(qname, type)
                }
            }
        }
        type
    }

    boolean isBuiltInType(def valueNode)
    {
        String typeName = valueNode.@type?.text()
        Type type = typeName ? schema.getGlobal(qname(typeName)) : null
        type ? type.builtInType : false
    }

    boolean isSingleValue(def simpleContent)
    {
        simpleContent.restriction?.enumeration?.list()?.isEmpty() && countAttributes(simpleContent) == 0
    }

    def findRestrictions(def node)
    {
        def simpleType
        def restrictions = []
        def typeElement = node
        while(typeElement) {
            restrictions = typeElement.restriction?.childNodes()?.findAll { n ->
                'enumeration' != n.qname //filter out enumerations as they are mapped to enum classes
            }
            if (restrictions) //are we done? found simpleType.restriction
                break
            def typeName = typeElement.@type?.text() //reference to simpleType?
            typeName = (typeName ?: typeElement.simpleContent?.extension?.@base?.text()) //nested simple type?
            typeElement = simpleTypeNodeLookup(typeName) //?: complexTypeNodeLookup[typeName]
        }
        restrictions
    }

    def setAttributeProperties(Attribute attribute, Object node)
    {
        try {
            attribute.required = node.@use?.text()=='required'
        } catch (Exception e) { e.printStackTrace() }
        String fixed = node.@fixed?.text() ?: null
        if (fixed) {
            attribute.fixed = fixed
            attribute.required = true
        }
        String _default = node.@default?.text() ?: null
        if (_default)
            attribute.setDefault(_default)
        String name = node.@name?.text()
        if (name)
            attribute.qname = qname(name)
        String typeWithPrefix = node.@type?.text()
        Type type = typeWithPrefix ? schema.getGlobal(qname(typeWithPrefix)) : null
        if (type)
            attribute.type = type
    }

    def setAnyProperties(Any any, Object node)
    {
        if (node.@namespace?.text())
            any.namespace = Value.Namespace.lookup( node.@namespace?.text() )
        if (node.@processContents?.text())
            any.processContents = Value.ProcessContents.valueOf( node.@processContents?.text() )
        setElementProperties(any, node)
    }

    def setElementProperties(Element element, Object node)
    {
        def minOccurs = node.@minOccurs?.text()
        element.minOccurs = minOccurs ? Integer.parseInt(minOccurs) : element.minOccurs
        def maxOccurs = node.@maxOccurs?.text()
        element.maxOccurs = maxOccurs ? (maxOccurs=='unbounded' ? Integer.MAX_VALUE : Integer.parseInt(maxOccurs)) : element.maxOccurs
        if (node.@default?.text())
            element.setDefault(node.@default?.text())
        if (node.@fixed?.text()) {
            element.fixed = node.@fixed?.text()
            if (!minOccurs) element.minOccurs = 1
        }
        element.setAbstract(node.@abstract?.text() == 'true')
        element.nillable = node.@nillable?.text() == 'true'
        String name = node.@name?.text()
        if (name)
            element.qname = qname(name)
        String typeWithPrefix = node.@type?.text()
        QName typeQName = (!typeWithPrefix && globalElementNodeLookup[element.qname]) ? element.qname : qname(typeWithPrefix)
        Type type = typeQName ? schema.getGlobal(typeQName) : null
        if (type) {
            element.type = type
        } else if ( ! (element instanceof Any) ) {
            println "no type for element=@name='${name}"
        }
    }

    boolean isTextOnlyType(def node)
    {
        countAttributes(node) == 0
    }

    int countAttributes(def node)
    {
        int count = node.attribute.list()?.size() ?: 0
        if (node.simpleContent?.extension?.attributeGroup) {
            for(attrGroup in node.simpleContent.extension.attributeGroup.list()) {
                //println attrGroup.@ref.text()
                def name = attrGroup.@ref.text()
                assert name != null
                //println "name = ${name}"
                def group = attributeGroupNodeLookup[name]
                //println "${node.name()}.simpleContent.extension.attributeGroup: ${group.@name.text()}"
                assert group != null
                count += group.attribute.list()?.size() ?: 0
            }
        }
        for (attrGroup in node.attributeGroup?.list()) {
            def name = attrGroup.@ref.text()
            println "name = ${name}"
            def group = attributeGroupNodeLookup[name]
            count += group.attribute.list()?.size() ?: 0
        }
        def any = node.anyAttribute?.list()
        count += (any && !any.isEmpty()) ? 1 : 0
        return count
    }

    Type findParentType(def parentNode)
    {
        while ( parentNode != null ) {
            if (parentNode.name() == 'complexType') {
                return elementStack.peek()
            } else if (parentNode.name() == 'simpleContent') {
                return simpleTypeStack.peek()
            } else if (parentNode.name() == 'simpleType') {
                return simpleTypeStack.peek()
            }
            parentNode = parentNode.parent()
        }
        throw new IllegalStateException("can't find parent Type")
    }
    Schema buildSchema(URL xmlSchemaURL) {
        prefixToNamespaceMap.push(loadNamespaces(xmlSchemaURL))
        Schema schema = buildSchema(xmlSchemaURL.openStream(), xmlSchemaURL)
        if (prefixToNamespaceMap.size() > 1) {
            prefixToNamespaceMap.pop()
        }
        schema
    }
    Schema buildSchema(String xmlSchemaText) {
        prefixToNamespaceMap.push(loadNamespaces(xmlSchemaText))
        Schema schema = buildSchema(new ByteArrayInputStream(xmlSchemaText.getBytes()), null)
        if (prefixToNamespaceMap.size() > 1) {
            prefixToNamespaceMap.pop()
        }
        schema
    }
    /**
     * work flow of normalizer
     */
    Schema buildSchema(InputStream inputStream, URL context)
    {
        boolean isRootSchema = schema == null
        XmlSlurper xmlSlurper = new XmlSlurper()
        //xmlSlurper.setFeature('http://apache.org/xml/features/disallow-doctype-decl', false)
        GPathResult xmlSchema = xmlSlurper
                .parse(inputStream)
                .declareNamespace(prefixToNamespaceMap.peek())
        if (isRootSchema) {
            schema = new Schema(prefixToNamespaceMap:prefixToNamespaceMap.peek())
            elementStack.push(schema)
            attributeStack.push(schema)
        } else {
            schema.prefixToNamespaceMap = prefixToNamespaceMap.peek()
        }
        for(imprt in xmlSchema.'import'.list()) {
            String location = imprt.@schemaLocation.text()
            //String namespace = imprt.@namespace.text()
            URL url = new URL(context, location)
            buildSchema(url)
        }
        indexGlobalNodes(xmlSchema)
        traverseElements(xmlSchema)
        if (isRootSchema) {
            if (!schema.rootElements) //if user has not defined root elements
                schema.rootElements = schema.elements.collect{ it.qname } //make all global elements root
        }
        schema
    }

    def static main(args)
    {
        //def schemaURL = new File('example-gpx-java/src/main/resources/gpx.xsd').toURI().toURL()
        //def schemaURL = new URL('http://www.topografix.com/gpx/1/1/gpx.xsd')
        //this.schemaURL = new URL('http://docs.oasis-open.org/election/external/xAL.xsd')
        //def schemaURL = new File('example-x-java/src/main/resources/xAL.xsd').toURI().toURL()
        //XmlSchemaNormalizer xmlSchemaNormalizer = new XmlSchemaNormalizer()
        //Schema schema = xmlSchemaNormalizer.buildSchema(schemaURL)
        //??.visit(xml)
        //println walker.xml
    }


}
