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

import com.javagen.schema.common.Gen
import com.javagen.schema.common.MappingUtil
import com.javagen.schema.common.PluralService
import com.javagen.schema.model.*
import com.javagen.schema.xml.QName
import com.javagen.schema.xml.XmlNodeCallback
import com.javagen.schema.xml.XmlSchemaNormalizer
import com.javagen.schema.xml.XmlSchemaVisitor
import com.javagen.schema.xml.node.*

import java.util.function.BiFunction

import static com.javagen.schema.common.GlobalFunctionsUtil.upperCase
import static com.javagen.schema.model.MCardinality.*
import static com.javagen.schema.model.MMethod.IncludeProperties.allProperties
import static com.javagen.schema.model.MMethod.Stereotype.*
import static com.javagen.schema.xml.node.Schema.DEFAULT_NS

import groovy.util.logging.Log

/**
 * Translate XML schema to Java 1.8 code.
 *
 * <p>A XmlNodeCallback can be used to apply specific third-party library annotations to the object model (see
 * JavaJacksonCallback as an example), allowing one to easily switch technologies.
 *
 * This class is the entry point for Java code generation.
 *
 *
 * @author Richard Easterling
 */
@Log
class JavaGen extends Gen implements XmlSchemaVisitor
{
    XmlNodeCallback callback

    Map<String,MModule> moduleMap = new LinkedHashMap<>()
    Map<String,MClass> classMap = [:]
    Stack<MSource> nestedStack = []
    Stack<Compositor> compositorStack = []
    Set<String> rootElements = [] as Set
    boolean rootElementsDefined = false
    MModule rootModule
    static final Set<String> CONTAINER_TYPES = ['NMTOKENS','IDREFS','ENTITIES'] as Set
    static final Compositor DEFAULT_COMPOSTER = new Sequence()
    Schema schema
    BiFunction<QName,MCardinality,String> propertyFromAttributeFunction = { qn, c -> propertyNameFunction.apply(qn.name)  }
    BiFunction<QName,MCardinality,String> propertyFromElementFunction = { qn, c -> c == LIST ? collectionNameFunction.apply(qn.name) : propertyNameFunction.apply(qn.name) }

    String anyAttributeName = 'anyAttribute'
    String anyAttributeType = 'string'
    String bodyPropertyName = 'text'
    String enumValueFieldName = 'value'
    String anyPropertyName = 'any'
    String anyPropeertyNameWrapped = 'map'
    String polyMorphicListName = 'list'
    String propertyScope = 'private'
    String anyType = 'string'
    boolean treatWrapperElementsAsCollections = true
    boolean choiceCollectionWrapperConstructor = true


    JavaGen(boolean skipInit=false)
    {
        super()
        //Java-specific config:
        srcFolder = 'src/main/java-gen'
        fileExtension = 'java'

        if (!skipInit) {
            //assign simpleXmlTypeToPropertyType function to lambda
            this.simpleXmlTypeToPropertyType = { typeName ->
                JavaTypeRegistry.simpleXmlTypeToPropertyType[typeName]
            }
            if ( ! MTypeRegistry.isInitialized() )
                new JavaTypeRegistry()
            callback = new JavaJacksonCallback(this)
            pipeline = [
                    new JavaPreEmitter(gen: this),
                    new JavaEmitter(gen: this)
            ]
        }
    }

    @Override def gen()
    {
        if (!customPluralMappings.isEmpty())
            pluralService = new PluralService(customPluralMappings) //pickup custom map
        schema = new XmlSchemaNormalizer().buildSchema(schemaURL)
        if (printSchema)
            println(schema)
        MDocument.source = schemaURL.toExternalForm()
        MDocument.namespace = schema.prefixToNamespaceMap[Schema.targetNamespace]
        visit(schema)
        super.gen()
    }

    @Override MModule getModel()
    {
        return rootModule
    }

    @Override def visit(Schema schema)
    {
        this.schema = schema
        rootElementsDefined = !rootElements.isEmpty()
        String name = packageNameFunction.apply(schema.prefixToNamespaceMap[Schema.targetNamespace])
        rootModule = new MModule(name:name)
        this << rootModule
        XmlSchemaVisitor.super.preVisit(schema) //visit global elements, pre-create classes for type reference lookups
        XmlSchemaVisitor.super.visit(schema)
        this >> rootModule
        callback.gen(schema, rootModule)
    }

    /** pre-create global classes */
    @Override def preVisit(SimpleType simpleType)
    {
        String className = classNameFunction.apply(simpleType.qname.name)
        MClass clazz = new MClass(name: className)
        this << clazz
        this >> clazz
    }
    /** pre-create global classes */
    @Override def preVisit(ComplexType complexType)
    {
        String className = classNameFunction.apply(complexType.qname.name)
        MClass clazz = new MClass(name: className)
        this << clazz
        this >> clazz
    }
    /** pre-create global enum classes */
    @Override def preVisit(TextOnlyType textOnlyType)
    {
        if (mapToEnum(textOnlyType)) {
            String className = enumClassName(textOnlyType.qname.name)
            MClass clazz = new MEnum(name:className)
            this << clazz
            this >> clazz
        }
    }


    @Override def visit(Any any)
    {
//      any.type = xml.getGlobal(DEFAULT_NS, anyType) - can't modify after vistor activated - causes bugs
//		println "any:${any.type.qname.name} -> ${name} property"
        TextOnlyType parentType = nestedStack.peek().attr['nodeType']
        if (parentType.qname.name  == 'extensionsType')
            println parentType
        //Compositor compositor = compositorStack.peek()
        boolean isBody = parentType.isBody()
        boolean isWrapper = parentType.isWrapperElement(true)
        if ( !isBody && !isWrapper) {
            String name = parentType.isWrapperElement(true) ?  anyPropeertyNameWrapped : anyPropertyName
            genAny(name, any)
        } else if (isWrapper) {
            anyWrapper(any)
        } else {
            genAny(anyPropertyName, any)
        }
    }

    void anyWrapper(Any any)
    {
        MCardinality container = container(any)
        if (container.isContainer())
            container = LINKEDMAP
        String name = container.isContainer() ? anyPropertyName : anyPropeertyNameSingular
        Type schemaType = polymporphicType(any)
        String type = schemaTypeToPropertyType(schemaType ?: schema.getGlobal(DEFAULT_NS,anyType), container)
        MProperty property = new MProperty(name:name, type:type, scope:propertyScope)
        setNotNull(property)
        optionalToPrimitiveWrapper(property)
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        callback.gen(any, property)
        //println "any -> ${name}: ${type}"
    }
    @Override def visit(AnyAttribute anyAttribute)
    {
        //println "anyAttribute @name=${anyAttribute?.qname?.name ?: anyAttributeName}"
        MCardinality container = container(anyAttribute)
        String name = propertyNameFunction.apply(anyAttribute?.qname?.name ?: anyAttributeName)
        String type = schemaTypeToPropertyType(anyAttribute.type ?: schema.getGlobal(DEFAULT_NS,anyAttributeType), container)
        MProperty property = new MProperty(name:name, type:type, scope:propertyScope)
        setNotNull(property)
        optionalToPrimitiveWrapper(property)
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        callback.gen(anyAttribute, property)
    }
    @Override def visit(Attribute attribute)
    {
        //println "attribute @name=${attribute.qname.name} @type=${attribute.type}"
        MCardinality container = container(attribute)
        String name = propertyFromAttributeFunction.apply(attribute.qname, container)
        if (!attribute.type)
            attribute.type = schema.getGlobal(DEFAULT_NS, 'anySimpleType')
        MType type = schemaTypeToPropertyType(attribute.type, container)
        String val = attribute.fixed ?: attribute.'default'
        java.util.List<MRestriction> restrictions = MappingUtil.translate(attribute)
        MProperty property = new MProperty(name:name, type:type, cardinality:container, scope:propertyScope, final:attribute.fixed!=null, val:val, restrictions:restrictions)
        setNotNull(property)
        optionalToPrimitiveWrapper(property)
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        callback.gen(attribute, property)
    }
    @Override def visit(AttributeGroup attributeGroup)
    {
        //println "attributeGroup @name=${attributeGroup.qname.name}"
    }
    @Override def visit(Element element)
    {
        MClass clazz = nestedStack.peek()
        def parentNode = clazz.attr['nodeType']
//        if (element.qname.name == 'color'){//} && clazz.name == 'Trkseg'){//&& textOnlyType.qname.name == ' Trkseg') {
//            println "element @name=${element.qname.name} @type=${element.type} -> ${clazz.name}"
//        }
        //Compositor compositor = compositorStack.peek()
        MCardinality container = container(element)
        String name = propertyFromElementFunction.apply(element.qname, container)
        MType type = null
        if (element.type) {
            type = schemaTypeToPropertyType(element.type, container)
            if (!type)
                throw new IllegalStateException("no type for element: ${element}")
            if (element.type.isWrapperElement()) {
                Type wrappedType = element.type.wrapperType()
                if (wrappedType) {
                    container = LIST
                    type = schemaTypeToPropertyType(wrappedType, container)
                } else if (element.type.childElements()[0] instanceof Any) {
                    Any any = element.type.childElements()[0]
                    genAny(name, any)
                    return
                }
            }
        } else {
            if (element.isAbstract()) {
                type = schemaAbstractTypeToPropertyType(element)
            } else {
                throw new IllegalStateException("no type for element: ${element} with parent: ${parentNode}")
            }
        }
        String val = element.fixed ?: element.'default'
        java.util.List<MRestriction> restrictions = MappingUtil.translate(element)
        MProperty property = new MProperty(name:name, type:type, cardinality:container, scope:propertyScope, final:element.fixed!=null, val:val, restrictions:restrictions)
        property.document = toDocument(element) ?: toDocument(element.type)
        setNotNull(property)
        optionalToPrimitiveWrapper(property)
        clazz.addField(property)
        callback.gen(element, property)
    }
    @Override def visit(Body body)
    {
        println "body @type=${body.type} @mixed=${body.mixedContent}"
        if (body.mixedContent) println "WARNING: mixed content currently not supported for body: ${body}"
        MCardinality container = container(body)
        String name = propertyNameFunction.apply(bodyPropertyName)
        MType type = schemaTypeToPropertyType(body.type ?: schema.getGlobal(DEFAULT_NS, 'string'), container)
        MProperty property = new MProperty(name:name, type:type, cardinality: container, scope:propertyScope, attr:['body':name])
        setNotNull(property)
        optionalToPrimitiveWrapper(property)
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        callback.gen(body, property)
    }
    @Override def visit(ComplexType complexType)
    {
        String className = classNameFunction.apply(complexType.qname.name)
        MClass clazz = lookupOrCreateClass(className)
        clazz.attr['nodeType'] = complexType
        if (complexType.qname.name == 'seasonType')
            println clazz.name
        compositorStack.push(DEFAULT_COMPOSTER)
        this << clazz
        XmlSchemaVisitor.super.visit(complexType)
        this >> clazz
        setClassProperties(clazz, complexType)
        callback.gen(complexType, clazz)
        compositorStack.pop()
    }

    def setClassProperties(MClass clazz, ComplexType complexType)
    {
//		if (clazz.name == 'Leg')
//			println "ComplexType -> class: ${clazz.name}"
        clazz.ignore = treatWrapperElementsAsCollections ? complexType.isWrapperElement() : false
        if (clazz.ignore)
            log.warning "IGNORING WRAPPER CLASS: ${clazz.name}".toString()
        java.util.List<String> ignore = complexType.appinfoValues(Appinfo.IGNORE_DIRECTIVE)
        if (ignore && !ignore.isEmpty())
            clazz.ignore = true
        clazz.abstract = complexType.abstract
        if (complexType.base) {
            String typeName = complexType.base.qname.name
//			if (typeName == 'mediaReferenceType')
//				println 'mediaReferenceType'
            String className = classNameFunction.apply(typeName)
            clazz.extends = className
        }
        java.util.List<String> extensions = complexType.appinfoValues(Appinfo.EXTENDS_DIRECTIVE)
        for(String extension : extensions) {
            String typeName = classNameFunction.apply(extension)
            if (clazz.extends)
                log.warning "CLASS ${clazz.name} ALLREADY extends ${clazz.extends}, overriding extends with ${typeName}"
            clazz.extends = typeName
        }
        java.util.List<String> interfaces = complexType.appinfoValues(Appinfo.IMPLEMENTS_DIRECTIVE)
        for(String _interface : interfaces) {
            String interfaceName = classNameFunction.apply(_interface)
            //if (clazz.extends)
            //	log.warning "CLASS ${clazz.name} ALLREADY extends ${clazz.extends}, overriding extends with ${interfaceName}"
            clazz.implements << interfaceName
        }
        java.util.List<String> directives = complexType.appinfoValues(Appinfo.ABSTACTION_DIRECTIVE)
        for(String directive : directives) {
            switch (directive) {
                case 'interface':
                    println "INTERFACE FROM COMPLEX_TYPE: ${clazz.name}"
                    clazz.abstract = true
                    clazz.interface = true
                    break
                case 'mixin':
                    println "MIXIN FROM COMPLEX_TYPE: ${clazz.name}"
                    clazz.mixin = true
                    break
            }
        }
        clazz.document = toDocument(complexType)
    }

    protected MDocument toDocument(Node node)
    {
        if (node?.annotation?.documentation?.size()>0) {
            return new MDocument(statements: node.annotation.docLines())
        } else {
            return null
        }
    }

    @Override def visit(All all) {
        compositorStack.push(all)
        XmlSchemaVisitor.super.visit(all)
        compositorStack.pop()
    }

    private Map<MType,String> collectPolymorphicTypes(Choice choice)
    {
        Map<MType,String> polymorphicTypes = new LinkedHashMap()
        for(Element e : choice.childElements()) { //includes elements and groups
            if (e.type == null)
                throw new Error("No mapped type for ${e}")
            MType type = schemaTypeToPropertyType(e.type, REQUIRED)
            polymorphicTypes.put(type, e.qname.name)
        }
        polymorphicTypes
    }


    private void choiceCollectionWrapper(Choice choice)
    {
        MCardinality container = LIST
        String name = polyMorphicListName
        Type schemaType = polymporphicType(choice)
        MType type = schemaTypeToPropertyType(schemaType ?: schema.getGlobal(DEFAULT_NS,anyType), container)
        MProperty property = new MProperty(name:name, type:type, cardinality:container, scope:propertyScope, polymorphicTypes:collectPolymorphicTypes(choice))
        setNotNull(property)
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        if (choiceCollectionWrapperConstructor) {
            MMethod constructor = new MMethod(stereotype:constructor, includeProperties: MMethod.IncludeProperties.allProperties)
            constructor.params << new MBind(name:name, type:type, cardinality:container)
            clazz.addMethod(constructor)
        }
        callback.gen(choice, property)
        println "choice -> ${name}: ${type}"
    }

    private void choicePolymprphicInstance(Choice choice)
    {
        MCardinality container = choice.minOccurs==1 ? REQUIRED : OPTIONAL
        String name = 'value' //TODO dig up proper name
        Type schemaType = polymporphicType(choice)
        MType type = schemaTypeToPropertyType(schemaType ?: schema.getGlobal(DEFAULT_NS,anyType), container)
        MProperty property = new MProperty(name:name, type:type, cardinality:container, scope:propertyScope, polymorphicTypes:collectPolymorphicTypes(choice))
        setNotNull(property)
        optionalToPrimitiveWrapper(property)
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        callback.gen(choice, property)
        println "choice -> ${name}: ${type}"
    }

    @Override def visit(Choice choice)
    {
        Compositor parent = compositorStack.peek()
        compositorStack.push(choice)
        TextOnlyType parentType = nestedStack.peek().attr['nodeType']
        switch (choice.polymorphicType()) {
            case Choice.Polymorphic.COLLECTION:
                choiceCollectionWrapper(choice)
                break
            case Choice.Polymorphic.SINGLE_VALUE:
                choicePolymprphicInstance(choice)
                break
            case Choice.Polymorphic.NOT_POLYMORPHIC:
                println "WARNING: ${parentType?.qname.name} choice element will be treated as a sequence, not a polymorphic instance or collection"
                XmlSchemaVisitor.super.visit(choice)
                break
        }
        compositorStack.pop()
    }
    @Override def visit(Sequence sequence)
    {
        compositorStack.push(sequence)
        XmlSchemaVisitor.super.visit(sequence)
        compositorStack.pop()
    }

    @Override def visit(Group group)
    {
        println "group @name=${group.qname.name}"
        java.util.List<String> directives = group.appinfoValues(Appinfo.ABSTACTION_DIRECTIVE)
        for(String directive : directives) {
            switch (directive) {
                case 'interface':
                    String className = classNameFunction.apply(group.qname.name)
                    println "INTERFACE FROM GROUP: ${className}"
                    MClass clazz = lookupOrCreateClass(className)
                    clazz.abstract = true
                    clazz.interface = true
                    clazz.attr['nodeType'] = group
                    java.util.List<String> extensions = group.appinfoValues(Appinfo.EXTENDS_DIRECTIVE)
                    for(String extension : extensions) {
                        String interfaceName = classNameFunction.apply(extension)
                        if (clazz.extends)
                            log.warning "CLASS ${className} ALLREADY extends ${clazz.extends}, overriding extends with ${interfaceName}"
                        clazz.extends = interfaceName
                        println "INTERFACE EXTENDING ${className} GROUP: ${interfaceName}"
                    }
//					if (clazz.name == 'Results')
//						println clazz.name
                    compositorStack.push(group)
                    this << clazz
                    XmlSchemaVisitor.super.visit(group)
                    this >> clazz
                    clazz.document = toDocument(group)
                    //setClassProperties(clazz, group)
                    //callback.gen(group, clazz)
                    compositorStack.pop()
                    break
                case 'mixin':
                    String className = classNameFunction.apply(group.qname.name)
                    println "MIXIN FROM GROUP: ${className}"
                    MClass clazz = lookupOrCreateClass(className)
                    clazz.mixin = true
                    clazz.attr['nodeType'] = group
                    java.util.List<String> extensions = group.appinfoValues(Appinfo.EXTENDS_DIRECTIVE)
                    for(String extension : extensions) {
                        String interfaceName = classNameFunction.apply(extension)
                        if (clazz.extends)
                            log.warning "MIXIN ${className} ALLREADY extends ${clazz.extends}, overriding extends with ${interfaceName}"
                        clazz.extends = interfaceName
                        println "MIXIN EXTENDING ${className} GROUP: ${interfaceName}"
                    }
//					if (clazz.name == 'Results')
//						println clazz.name
                    compositorStack.push(group)
                    this << clazz
                    XmlSchemaVisitor.super.visit(group)
                    this >> clazz
                    clazz.document = toDocument(group)
                    //setClassProperties(clazz, group)
                    //callback.gen(group, clazz)
                    compositorStack.pop()
                    break
                default:
                    log.warning("unknown appinfo directive: '${directive}' on ${group.qname.name} group")
            }
        }
    }
    @Override def visit(List list)
    {
        println "list @itemType=${list.itemType}"
    }
    @Override def visit(SimpleType simpleType)
    {
        //String name = simpleType.qname.name
//		if (name == 'mediaReferenceType' || name == 'phoneNumberType')
//			println name
        String className = classNameFunction.apply(simpleType.qname.name)
        MClass clazz = lookupOrCreateClass(className)
        clazz.attr['nodeType'] = simpleType
        this << clazz
        XmlSchemaVisitor.super.visit(simpleType)
        this >> clazz
        clazz.document = toDocument(simpleType)
        if (simpleType.isInheritedBaseType()) {
            String typeName = simpleType.base.qname.name
//			if (typeName == 'mediaReferenceType')
//				println 'mediaReferenceType'
            String baseClassName = classNameFunction.apply(typeName)
            clazz.extends = baseClassName
        }

        callback.gen(simpleType, clazz)
    }
    @Override def visit(TextOnlyType textOnlyType)
    {
        if (mapToEnum(textOnlyType)) {
            MEnum clazz = null
            if (textOnlyType.base instanceof Union) {
                clazz = genUnion(textOnlyType) as MEnum
            } else {
                String className = enumClassName(textOnlyType.qname.name)
                if (textOnlyType.restrictionSet().size() > 1) {
                    println "WARNING: can't model ${textOnlyType.qname.name} complex enum class, ignoring non-enum restrictions: ${textOnlyType.restrictionSet()}"
                }
                def enumValues = textOnlyType.restrictions.findAll{ it.type == Restriction.RType.enumeration }.collect{ it.value } //enumValueFunction.apply(
                clazz = lookupOrCreateClass(className, true) as MEnum
                clazz.enumValues = enumValues
                generateEnumClass( clazz )
            }
            if (!clazz) {
                throw new IllegalStateException("no clazz generated for TextOnlyType: ${textOnlyType}")
            }
            this << clazz
            //println "textOnlyType @name=${textOnlyType.qname.name} -> ${clazz}"
            this >> clazz
            clazz.document = toDocument(textOnlyType)
            callback.gen(textOnlyType, clazz)
        } else {
            //println "textOnlyType @name=${textOnlyType.qname.name} -> will map to simple type: ${textOnlyType}, cardinality:${REQUIRED}"
        }
    }

    @Override def visit(Union union)
    {
        println "union @name=${union.qname.name}"
        union.simpleTypes.eachWithIndex { TextOnlyType type, int i ->
            "${i==0 ? ':' : ','} ${type.qname?.name}"
        }
        println()
    }
    @Override def visit(QName root)
    {
        Element rootElement = schema.lookupElement(root)
        String className = classNameFunction.apply(rootElement.type.qname.name)
        MClass clazz = rootModule.lookupClass(className)
        int count = 0
        if (!rootElementsDefined || rootElements.contains(rootElement.qname.name)) {
            //println "root node: ${root}"
//			if (clazz.name == 'AddressDetails') {
//				println "${clazz.name} #${count}"
//				printStackTrace()
//			}
            callback.gen(rootElement, clazz)
            count++
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    // xml methods
    ////////////////////////////////////////////////////////////////////////////

    protected Type polymporphicType(Compositor compositor)
    {
        String typeName = compositor.polymporphicRootTypeName() //looks for id="*polymorphic-{rootTypeName}"
        schema.getGlobal(typeName)
    }
    protected Type polymporphicType(Any any)
    {
        String typeName = any.polymporphicRootTypeName() //looks for id="*polymorphic-{rootTypeName}"
        schema.getGlobal(typeName)
    }

    void mapPropertyAccessors(MProperty property)
    {
        String propertyName = property.name
        property.methods[putter] = new MMethod(name: "put${upperCase(propertyName)}", params: [new MBind(name:'key', type: 'String'), new MBind(name: 'value', type: property.type)], body: JavaPreEmitter.&putterMethodBody, stereotype: putter, refs: ['property':property])
        //property.methods[getter] = new MMethod(name: "get${upperCase(propertyName)}", type:property.type, body: JavaPreEmitter.&getterMethodBody, stereotype: getter, refs: ['property':property])
    }

    MProperty genAny(String propertyName, Any any)
    {
        //println "any @name=${any.qname?.name}"
        MCardinality container = container(any)
        Type polymorphicType = polymporphicType(any) ?: any.type // any.type is not allowed in XML Schema?
        MType type = schemaTypeToPropertyType(polymorphicType ?: schema.getGlobal(DEFAULT_NS, anyType), container)
        MProperty property
        if (container == LIST) {
            container = LINKEDMAP
            MBind mapRType = new MBind(cardinality:container,type:type)
            MType linkedMapType = MTypeRegistry.instance().typeForCardinality(LINKEDMAP)
            String val = null //linkedMapType.val
            property = new MProperty(name:propertyName, type:type, cardinality:container, scope:propertyScope, final:any.fixed!=null, val:val, attr:['keyType':'String'])
            mapPropertyAccessors(property)
        } else if (polymorphicType) {
            MType listType = MTypeRegistry.instance().typeForCardinality(LIST)
            String val = any.fixed ?: any.'default' ?: (container == LIST) ? listType.val : null
            property = new MProperty(name:propertyName, type:type, cardinality:container, scope:propertyScope, final:any.fixed!=null, val:val)
        } else {
            String val = any.fixed ?: any.'default'
            property = new MProperty(name:propertyName, type:type, cardinality:container, scope:propertyScope, final:any.fixed!=null, val:val)
        }
        setNotNull(property)
        optionalToPrimitiveWrapper(property)
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        callback.gen(any, property)
        property
    }

    /** enumerations are mapped to Java enum classes */
    boolean mapToEnum(TextOnlyType textOnlyType)
    {
        if (textOnlyType.base instanceof Union) {
            Union union = textOnlyType.base as Union
            union.simpleTypes.every{ it.restrictionSet().contains(Restriction.RType.enumeration) }
        } else {
            textOnlyType.restrictionSet().contains(Restriction.RType.enumeration)
        }
    }

    MClass genUnion(TextOnlyType unionType)
    {
        Union union = unionType.base as Union
        //println "union @name=${unionType.qname.name}"
        boolean isEnumUnion = mapToEnum(unionType)
        if (isEnumUnion) {
            Set<String> enumValues = [] as Set
            union.simpleTypes.eachWithIndex { TextOnlyType type, int i ->
                "${i == 0 ? ':' : ','} ${type.qname?.name}"
                if (type.restrictionSet().contains(Restriction.RType.enumeration)) {
                    if (type.restrictionSet().size() > 1) {
                        println "WARNING: can't model ${unionType.qname.name} union member ${type.qname.name} as enum class, ignoring non-enum restrictions: ${type.restrictionSet()}"
                    }
                    type.restrictions.findAll{ it.type == Restriction.RType.enumeration }.each{ enumValues << it.value } //enumValueFunction.apply
                }
            }
            println()
            String className = enumClassNameFunction.apply(unionType.qname.name)
            MEnum mEnum =  lookupOrCreateEnum(className)
            mEnum.enumValues = new ArrayList(enumValues)
            generateEnumClass(mEnum)
        } else {
            println "TODO add support for non-enum unions, skipping ${union.qname.name}"
            null
        }
    }

    void addEnumValueSupport(MEnum enumClass)
    {
        //setup a private value addField
        enumClass.addField( new MProperty(name: enumValueFieldName, scope: 'private', 'final': true) )
        //add a private constructor
        enumClass.addMethod( new MMethod(name:enumClass.shortName(), stereotype:constructor, includeProperties:allProperties, scope:'private') )
    }

    MEnum generateEnumClass(MEnum enumClass)
    {
        java.util.List<String> enumValues = enumClass.enumValues.sort()
        def enumNames = []
        def enumNameSet = [] as Set //look for and fix duplicate names
        for(tag in enumValues) {
            def enumName = enumNameFunction.apply(tag)
            int i = 1
            while (enumNameSet.contains(enumName)) //name not unique?
                enumName += i
            enumNameSet << enumName
            enumNames << enumName
        }
        enumClass.enumNames = enumNames
        enumClass.enumValues = enumValues
        addEnumValueSupport(enumClass)
        enumClass
    }

    ////////////////////////////////////////////////////////////////////////////
    // internal methods
    ////////////////////////////////////////////////////////////////////////////

    String enumClassName(String tag)
    {
        enumClassNameFunction.apply(tag)
    }

    MClass lookupOrCreateClass(String className, boolean isEnum=false)
    {
        MSource parent = nestedStack.peek()
        MClass clazz = parent.lookupClass(className)
        boolean isNestedClass = parent instanceof MClass //nested classes should be static
        clazz ?: isEnum ? new MEnum(name: className) : new MClass(name: className, static: isNestedClass)
    }
    MEnum lookupOrCreateEnum(String enumName)
    {
        MEnum clazz = nestedStack.peek().lookupClass(enumName) as MEnum
        clazz ?: new MEnum(name: enumName)
    }

    MCardinality container(Attribute attribute)
    {
        Type type = attribute.type
        while(type) {
            switch (type) {
                case TextOnlyType:
                    if (CONTAINER_TYPES.contains(type.qname.name)) {
                        return LIST
                    }
                    break
                case List:
                    return LIST
                case Union:
                case SimpleType:
                case ComplexType:
                    break
                default:
                    throw new IllegalStateException("unhandled type in containerFromAttribute: ${attribute}")
            }
            if (type.isBuiltInType()) {
                break //exit while loop
            } else {
                type = type.base //go deeper looking for built-in type
            }
        }
        if (attribute.isRequired()) {
            return REQUIRED
        } else {
            return OPTIONAL
        }
    }
    MCardinality container(Element element)
    {
        Type type = element.type
        while(type) {
            switch (type) {
                case TextOnlyType:
                    if (type && CONTAINER_TYPES.contains(type.qname.name)) {
                        return LIST
                    }
                    break
                case List:
                    return LIST
                case Union:
                case SimpleType:
                case ComplexType:
                    break
                default:
                    throw new IllegalStateException("unhandled type in containerFromAttribute: ${element}")
            }
            if (type.isBuiltInType()) {
                break //exit while loop
            } else {
                type = type.base //go deeper looking for built-in type
            }
        }
        Compositor compositor = compositorStack.peek()
        if (compositor.maxOccurs > 1 && !compositor.isUboundedChildElement()) //can't use parent maxOccurs if any child is unbounded
            return LIST
        if (element.maxOccurs > 1) {
            return LIST
        } else if (element.minOccurs == 0) {
            return OPTIONAL
        } else {
            return REQUIRED
        }
    }
    MCardinality container(Body body) {
        Type type = body.type
        while (type) {
            switch (type) {
                case TextOnlyType:
                    if (type && CONTAINER_TYPES.contains(type.qname.name)) {
                        return LIST
                    }
                    break
                case List:
                    return LIST
                case Union:
                case SimpleType:
                case ComplexType:
                    break
                default:
                    throw new IllegalStateException("unhandled type in containerFromAttribute: ${element}")
            }
            if (type.isBuiltInType()) {
                break //exit while loop
            } else {
                type = type.base //go deeper looking for built-in type
            }
        }
        return OPTIONAL //TODO verify there is no way to make body required
    }
    /** warning: modifies model! */
    void optionalToPrimitiveWrapper(MProperty property) {
        if (!useOptional && OPTIONAL == property.cardinality) {
            property.cardinality = REQUIRED // just use nulled wrapper class
        }
    }
    void setNotNull(MProperty property)
    {
        property.attr['notNull'] = REQUIRED == property.cardinality && !property.type.isPrimitive()
    }
    MType schemaTypeToPropertyType(Type type, MCardinality container)
    {
        if (!type)
            throw new Error("Missing type")
//		if (type.qname.name == 'decimal')
//			print('decimal')
        String typeName = schemaTypeToPropertyTypeName(type) // defaults to primitives
        MType langType = MType.lookupType(typeName) //global type?
        if (!langType) {
            visit(type) //nested type?
            langType = nestedStack.peek().lookupClass(typeName)
        }
        if (!langType)
            throw new Error("No type registed for ${typeName}")
        if (container == OPTIONAL && langType.isPrimitive()) {
            String wrapper = JavaTypeRegistry.useWrapper(typeName)
            if (typeName != wrapper) {
                return MType.lookupType(wrapper)
            }
        }
        langType
    }
    /** Handle typeless abstract elements - lookup type, if not found create a new interface */
    MClass schemaAbstractTypeToPropertyType(Element element)
    {
        String tag = element.qname.name
        String className = classNameFunction.apply(tag)
        MType type = MTypeRegistry.instance().lookupType(className) //interface already created
        if (!type) {
            MModule parent = topModule()
            type = new MClass(name: className, interface: true, abstract: true, parent: parent)
            MTypeRegistry.instance().registerType(className, type)
            parent.classes << type
        } else if (!type.isInterface()) {
            throw new Error("class name conflict ${} creating element ${element}")
        }
        type
    }

    MModule topModule()
    {
        int i = nestedStack.size()
        MSource src = nestedStack.elementAt(i-1)
        while( ! (src instanceof MModule) ) {
            i--
            src = (MModule)nestedStack.elementAt(i-1)
        }
        i < 0 ? null : (MModule)nestedStack.elementAt(i-1)
    }
    String schemaTypeToPropertyTypeName(Type type)
    {
        //String javaType
        Type t = type
        if (t.qname.name=='uuidType')
            println t.qname.name
        while(t) {
            switch (t) {
                case SimpleType:
                case ComplexType:
                    return classNameFunction.apply(t.qname.name)
                case TextOnlyType:
                    if (mapToEnum(t as TextOnlyType)) {
                        return enumClassNameFunction.apply(t.qname.name)
                    } else { //if (t.isBuiltInType()) {
                        String langType = simpleXmlTypeToPropertyType.apply(t.qname.name)
                        if (langType) {
                            return langType
                        } else {
                            t = t.base //go deeper looking for built-in type
                        }
                    }
                    break
                case List:
                    t = t.itemType
                    break
                case Union:
                    t = t.simpleTypes[0] //try the first member
                    break
                default:
                    throw new IllegalStateException("not expecting ${t} converting type: ${type}")
            }
        }
        throw new IllegalStateException("TODO add support for converting type: ${type}")
    }

    MBase leftShift(MBase node) {
        if (node) {
            if (node instanceof MModule) {
                if (!nestedStack.isEmpty()) {
                    MModule parent = nestedStack.peek() as MModule
                    parent.child(node)
                }
                nestedStack << node
                if (node.name)
                    moduleMap[node.name] = node

            } else if (node instanceof MClass || node instanceof MEnum) {
                MSource parent = nestedStack.peek() //as MNested
                boolean isGlobalNode = node.name && (parent instanceof MModule)
                boolean shouldAdd = !isGlobalNode || !MType.lookupType(node.name)
                nestedStack << node
                //println "${parent} << ${node}, shouldAdd=${shouldAdd}, isGlobalNode=${isGlobalNode}, MType.lookupType(node.name)=${MType.lookupType(node.name)}"
                if (shouldAdd) {
                    parent.addClass(node)
                    if (node.name) {
                        classMap[node.name] = node
                        if (isGlobalNode) //if in global scope, register type
                            MType.registerType(node)
                    }
                }
            } else if (node instanceof MProperty || node instanceof MField || node instanceof MReference) {
                MClass clazz = nestedStack.peek() as MClass
                clazz.addField(node)
            } else if (node instanceof MMethod) {
                MClass clazz = nestedStack.peek() as MClass
                clazz.addMethod(node)
            }
        }
        node
    }
    MBase rightShift(MBase node) {
        if (node) {
            if (node instanceof MModule) {
                nestedStack.pop()
            } else if (node instanceof MClass || node instanceof MEnum) {
                nestedStack.pop()
//            } else if (node instanceof MProperty || node instanceof MField || node instanceof MReference) {
//            } else if (node instanceof MMethod) {
            }
        }
        node
    }
}
