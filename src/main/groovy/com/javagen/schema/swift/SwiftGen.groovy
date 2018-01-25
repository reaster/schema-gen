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

import com.javagen.schema.common.Gen
import com.javagen.schema.common.GlobalFunctionsUtil
import com.javagen.schema.common.PluralService
import com.javagen.schema.model.MBase
import com.javagen.schema.model.MBind
import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MField
import com.javagen.schema.model.MMethod
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.model.MReference
import com.javagen.schema.model.MSource
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry
import com.javagen.schema.xml.QName
import com.javagen.schema.xml.XmlNodeCallback
import com.javagen.schema.xml.XmlSchemaNormalizer
import com.javagen.schema.xml.XmlSchemaVisitor
import com.javagen.schema.xml.node.All
import com.javagen.schema.xml.node.Any
import com.javagen.schema.xml.node.AnyAttribute
import com.javagen.schema.xml.node.Attribute
import com.javagen.schema.xml.node.AttributeGroup
import com.javagen.schema.xml.node.Body
import com.javagen.schema.xml.node.Choice
import com.javagen.schema.xml.node.ComplexType
import com.javagen.schema.xml.node.Compositor
import com.javagen.schema.xml.node.Element
import com.javagen.schema.xml.node.Group
import com.javagen.schema.xml.node.List
import com.javagen.schema.xml.node.Restriction
import com.javagen.schema.xml.node.Schema
import com.javagen.schema.xml.node.Sequence
import com.javagen.schema.xml.node.SimpleType
import com.javagen.schema.xml.node.TextOnlyType
import com.javagen.schema.xml.node.Type
import com.javagen.schema.xml.node.Union

import java.util.function.BiFunction

import static com.javagen.schema.common.GlobalFunctionsUtil.lowerCase
import static com.javagen.schema.common.GlobalFunctionsUtil.upperCase
import static com.javagen.schema.model.MCardinality.LIST
import static com.javagen.schema.xml.node.Schema.DEFAULT_NS

/**
 * Translate XML schema to Swift 4 code.
 *
 * <p>A XmlNodeCallback can be used to apply specific third-party library annotations to the object model (or in this case
 * Apple's Codable API), allowing one to easily switch technologies.
 *
 * This class is the entry point for Swift 4 code generation.
 *
 * @author Richard Easterling
 */
class SwiftGen extends Gen implements XmlSchemaVisitor
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
	String anyType = 'string'
	boolean useOptional = false
	boolean useStruct = true
	String sourceFileName = null


	SwiftGen()
	{
		super()
		if ( ! MTypeRegistry.isInitialized() )
			new SwiftTypeRegistry()
		callback = new SwiftCodableCallback(this)
		pipeline = [
				new SwiftPreEmitter(gen: this),
				new SwiftEmitter(gen: this)
		]
		fileExtension = 'swift'
		srcDir = new File('src/main/swift-gen')
		simpleXmlTypeToPropertyType = { typeName -> SwiftTypeRegistry.simpleXmlTypeToPropertyType[typeName] }
		enumNameFunction = { text -> GlobalFunctionsUtil.swiftEnumName(text, false) }
		propertyNameFunction = { text -> GlobalFunctionsUtil.legalSwiftName(lowerCase(text)) }
		constantNameFunction = { text -> GlobalFunctionsUtil.swiftConstName(text) }
	}

	@Override def gen()
	{
//        schema = new XmlSchemaNormalizer().buildSchema(schemaURL)
//        visit(schema)
//        MModule rootModule = getModel()
//        if (!sourceFileName) //if no source file name defined, use first root element name
//            sourceFileName = schema.rootElements.isEmpty() ? 'JavaGen' : upperCase(schema.rootElements.first().name)
//        rootModule.sourceFile = pathFromSourceFileName(this, rootModule, sourceFileName)
//        pipeline.each { visitor ->
//            visitor.visit(rootModule)
//        }
        if (!customPluralMappings.isEmpty())
			pluralService = new PluralService(customPluralMappings) //pickup custom map
		schema = new XmlSchemaNormalizer().buildSchema(schemaURL)
		visit(schema)
		MModule rootModule = getModel()
		if (!sourceFileName) //if no source file name defined, use first root element name
			sourceFileName = schema.rootElements.isEmpty() ? 'JavaGen' : upperCase(schema.rootElements.first().name)
		rootModule.sourceFile = pathFromSourceFileName(this, rootModule, sourceFileName)
		pipeline.each { visitor ->
			visitor.visit(rootModule)
		}
	}

	@Override
	MModule getModel()
	{
		return rootModule
	}

	def visit(Schema schema)
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
	def preVisit(SimpleType simpleType)
	{
		String className = classNameFunction.apply(simpleType.qname.name)
		MClass clazz = new MClass(name: className)
		this << clazz
		this >> clazz
	}
	/** pre-create global classes */
	def preVisit(ComplexType complexType)
	{
		String className = classNameFunction.apply(complexType.qname.name)
		MClass clazz = new MClass(name: className)
		this << clazz
		this >> clazz
	}
	/** pre-create global enum classes */
	def preVisit(TextOnlyType textOnlyType)
	{
		if (mapToEnum(textOnlyType)) {
			String className = enumClassNameFunction.apply(textOnlyType.qname.name)
			MClass clazz = new MEnum(name:className)
			this << clazz
			this >> clazz
		}
	}

	def visit(All all) {
		compositorStack.push(all)
		XmlSchemaVisitor.super.visit(all)
		compositorStack.pop()
	}
	def visit(Choice choice) {
		compositorStack.push(choice)
		XmlSchemaVisitor.super.visit(choice)
		compositorStack.pop()
	}
	def visit(Sequence sequence) {
		compositorStack.push(sequence)
		XmlSchemaVisitor.super.visit(sequence)
		compositorStack.pop()
	}

	def visit(Any any)
	{
		String name = anyPropertyName
		any.type = schema.getGlobal(DEFAULT_NS, anyType)
		//println "any:${any.type.qname.name} -> ${name} property"
		TextOnlyType parentType = nestedStack.peek().attr['nodeType']
		if ( ! parentType.isBody() ) {
			genAny(name, any)
		}
	}
	def visit(AnyAttribute anyAttribute)
	{
		println "anyAttribute @name=${anyAttribute?.qname?.name ?: anyAttributeName}"
		MCardinality container = container(anyAttribute)
		String name = propertyNameFunction.apply(anyAttribute?.qname?.name ?: anyAttributeName)
		String type = schemaTypeToPropertyType(anyAttribute.type ?: schema.getGlobal(DEFAULT_NS,anyAttributeType), container)
		MProperty property = new MProperty(name:name, scope:'public', type:type)
		MClass clazz = nestedStack.peek()
		clazz.addField(property)
		callback.gen(anyAttribute, property)
	}
	def visit(Attribute attribute)
	{
		println "attribute @name=${attribute.qname.name} @type=${attribute.type}"
		MCardinality container = container(attribute)
		String name = propertyFromAttributeFunction.apply(attribute.qname, container)
		if (!attribute.type)
			attribute.type = schema.getGlobal(DEFAULT_NS, 'anySimpleType')
		MType type = schemaTypeToPropertyType(attribute.type, container)
		String val = attribute.fixed ?: attribute.'default'
		MProperty property = new MProperty(name:name, type:type, scope:'public', cardinality:container, final:attribute.fixed!=null, val:val)
		MClass clazz = nestedStack.peek()
		clazz.addField(property)
		callback.gen(attribute, property)
	}
	def visit(AttributeGroup attributeGroup)
	{
		println "attributeGroup @name=${attributeGroup.qname.name}"
	}
	def visit(Element element)
	{
		println "element @name=${element.qname.name} @type=${element.type}"
		MCardinality container = container(element)
		String name = propertyFromElementFunction.apply(element.qname, container)
		MType type = schemaTypeToPropertyType(element.type, container)
		if (!type)
			throw new IllegalStateException("no type for element: ${element}")
		if (element.type.isWrapperElement()) {
			Type wrappedType = element.type.wrapperType()
			if (wrappedType) {
				container = MCardinality.LIST
				type = schemaTypeToPropertyType(wrappedType, container)
			} else if (element.type.elements[0] instanceof Any) {
				Any any = element.type.elements[0]
				genAny(name, any)
				return
			}
		}
		String val = element.fixed ?: element.'default'
		MProperty property = new MProperty(name:name, type:type, cardinality:container, scope:'public', final:element.fixed!=null, val:val)
		MClass clazz = nestedStack.peek()
		clazz.addField(property)
		callback.gen(element, property)
	}
	def visit(Body body)
	{
		println "body @type=${body.type} @mixed=${body.mixedContent}"
		if (body.mixedContent) println "WARNING: mixed content currently not supported for body: ${body}"
		MCardinality container = container(body)
		String name = propertyNameFunction.apply(bodyPropertyName)
		String type = schemaTypeToPropertyType(body.type ?: 'string', container)
		MProperty property = new MProperty(name:name, type:type, scope:'public', attr: ['body':name])
		MClass clazz = nestedStack.peek()
		clazz.addField(property)
		callback.gen(body, property)
	}
	def visit(ComplexType complexType)
	{
		String className = classNameFunction.apply(complexType.qname.name)
		MClass clazz = lookupOrCreateClass(className)
		clazz.attr['nodeType'] = complexType
		compositorStack.push(DEFAULT_COMPOSTER)
		this << clazz
		XmlSchemaVisitor.super.visit(complexType)
		this >> clazz
		clazz.ignore = complexType.isWrapperElement()
		callback.gen(complexType, clazz)
		compositorStack.pop()
	}
	def visit(Group group)
	{
		println "group @name=${group.qname.name}"
	}
	def visit(List list)
	{
		println "list @itemType=${list.itemType}"
	}
	def visit(SimpleType simpleType)
	{
		String className = classNameFunction.apply(simpleType.qname.name)
		MClass clazz = lookupOrCreateClass(className)
		clazz.attr['nodeType'] = simpleType
		this << clazz
		XmlSchemaVisitor.super.visit(simpleType)
		this >> clazz
		callback.gen(simpleType, clazz)
	}
	def visit(TextOnlyType textOnlyType)
	{
		if (mapToEnum(textOnlyType)) {
			MEnum clazz = null
			if (textOnlyType.base instanceof Union) {
				clazz = genUnion(textOnlyType)
			} else {
				String className = enumClassNameFunction.apply(textOnlyType.qname.name)
				if (textOnlyType.restrictionSet().size() > 1) {
					println "WARNING: can't model ${textOnlyType.qname.name} complex enum class, ignoring non-enum restrictions: ${textOnlyType.restrictionSet()}"
				}
				def enumValues = textOnlyType.restrictions.findAll{ it.type == Restriction.RType.enumeration }.collect{ enumValueFunction.apply(it.value) }
				clazz = lookupOrCreateClass(className, true)
				clazz.enumValues = enumValues
				swiftEnum( clazz )
			}
			this << clazz
			println "textOnlyType @name=${textOnlyType.qname.name} -> ${clazz}"
			this >> clazz
			callback.gen(textOnlyType, clazz)
		} else {
			println "textOnlyType @name=${textOnlyType.qname.name} -> will map to simple type: ${textOnlyType}, cardinality:${MCardinality.REQUIRED}"
		}
	}
	def visit(Union union)
	{
		println "union @name=${union.qname.name}"
		union.simpleTypes.eachWithIndex { SimpleType type, int i ->
			"${i==0 ? ':' : ','} ${type.qname?.name}"
		}
		println()
	}
	def visit(QName root)
	{
		Element rootElement = schema.lookupElement(root)
		String className = classNameFunction.apply(rootElement.type.qname.name)
		MClass clazz = rootModule.lookupClass(className)
		if (!rootElementsDefined || rootElements.contains(rootElement.qname.name)) {
			println "root node: ${root}"
			callback.gen(rootElement, clazz)
		}
	}

	////////////////////////////////////////////////////////////////////////////
	// xml methods
	////////////////////////////////////////////////////////////////////////////

	MProperty genAny(String propertyName, Any any)
	{
		println "any @name=${any.qname?.name}"
		MCardinality container = container(any)
		Type xtype = any.type ?: schema.getGlobal(DEFAULT_NS, anyType)
		MType type = schemaTypeToPropertyType(xtype, container)
		MProperty property
		if (container == MCardinality.LIST) {
			container = MCardinality.MAP
			MBind mapRType = new MBind(cardinality:container,type:type)
			String val = '[:]'
			property = new MProperty(name:propertyName, type:type, cardinality:container, final:any.fixed!=null, scope:'public', val:val, attr:['keyType':'String'])
			//property.methods[putter] = new MMethod(name: "put${upperCase(propertyName)}", params: [new MBind(name:'key', type: 'String'), new MBind(name: 'value', type: type)], body: PreJavaVisitor.&putterMethodBody, stereotype: putter, refs: ['property':property])
			//property.methods[getter] = new MMethod(name: "get${upperCase(propertyName)}", type:mapRType, body: PreJavaVisitor.&getterMethodBody, stereotype: getter, refs: ['property':property])
		} else {
			String val = any.fixed ?: any.'default'
			property = new MProperty(name:propertyName, type:type, cardinality:container, scope:'public', final:any.fixed!=null, val:val)
		}
		MClass clazz = nestedStack.peek()
		clazz.addField(property)
		callback.gen(any, property)
		property
	}

	/** enumerations are mapped to Swift enum classes */
	boolean mapToEnum(TextOnlyType textOnlyType)
	{
		if (textOnlyType.base instanceof Union) {
			Union union = textOnlyType.base
			union.simpleTypes.every{ it.restrictionSet().contains(Restriction.RType.enumeration) }
		} else {
			textOnlyType.restrictionSet().contains(Restriction.RType.enumeration)
		}
	}

	MClass genUnion(TextOnlyType unionType)
	{
		Union union = unionType.base
		println "union @name=${unionType.qname.name}"
		boolean isEnumUnion = mapToEnum(unionType)
		if (isEnumUnion) {
			Set<String> enumValues = [] as Set
			union.simpleTypes.eachWithIndex { TextOnlyType type, int i ->
				"${i == 0 ? ':' : ','} ${type.qname?.name}"
				if (type.restrictionSet().contains(Restriction.RType.enumeration)) {
					if (type.restrictionSet().size() > 1) {
						println "WARNING: can't model ${unionType.qname.name} union member ${type.qname.name} as enum class, ignoring non-enum restrictions: ${type.restrictionSet()}"
					}
					type.restrictions.findAll{ it.type == Restriction.RType.enumeration }.each{ enumValues << enumValueFunction.apply(it.value) }
				}
			}
			println()
			String className = enumClassNameFunction.apply(unionType.qname.name)
			MEnum mEnum =  lookupOrCreateEnum(className)
			mEnum.enumValues = enumValues
			swiftEnum(mEnum)
		} else {
			println "TODO add support for non-enum unions, skipping ${union.qname.name}"
			null
		}
	}
	MEnum swiftEnum(MEnum enumClass)
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
		//setup a private value addField
		//enumClass.addField( new MProperty(name: enumValueFieldName, scope: 'private', 'final': true) )
		//add a private constructor
		//enumClass.addMethod( new MMethod(name:enumClass.shortName(), stereotype:constructor, includeProperties:allProperties, scope:'private') )
		enumClass
	}

	////////////////////////////////////////////////////////////////////////////
	// internal methods
	////////////////////////////////////////////////////////////////////////////

	MClass lookupOrCreateClass(String className, boolean isEnum=false)
	{
		MClass clazz = nestedStack.peek().lookupClass(className)
		clazz ?: isEnum ? new MEnum(name: className) : new MClass(name: className)
	}
	MEnum lookupOrCreateEnum(String enumName)
	{
		MEnum clazz = nestedStack.peek().lookupClass(enumName)
		clazz ?: new MEnum(name: enumName)
	}

	MCardinality container(Attribute attribute)
	{
		Type type = attribute.type
		while(type) {
			switch (type) {
				case TextOnlyType:
					if (CONTAINER_TYPES.contains(type.qname.name)) {
						return MCardinality.LIST
					}
					break
				case List:
					return MCardinality.LIST
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
			return MCardinality.REQUIRED
		} else {
			return MCardinality.OPTIONAL
		}
	}
	MCardinality container(Element element)
	{
		Type type = element.type
		while(type) {
			switch (type) {
				case TextOnlyType:
					if (type && CONTAINER_TYPES.contains(type.qname.name)) {
						return MCardinality.LIST
					}
					break
				case List:
					return MCardinality.LIST
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
		if (compositor.maxOccurs > 1  && !compositor.isUboundedChildElement()) //can't use parent maxOccurs if any child is unbounded
			return MCardinality.LIST
		if (element.maxOccurs > 1) {
			return MCardinality.LIST
		} else if (element.minOccurs == 0) {
			return MCardinality.OPTIONAL
		} else {
			return MCardinality.REQUIRED
		}
	}
	MCardinality container(Body body) {
		Type type = body.type
		while (type) {
			switch (type) {
				case TextOnlyType:
					if (type && CONTAINER_TYPES.contains(type.qname.name)) {
						return MCardinality.LIST
					}
					break
				case List:
					return MCardinality.LIST
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
	}
//    /** warning: modifies model! */
//    void optionalToPrimitiveWrapper(MProperty property) {
//        if (!useOptional && MCardinality.OPTIONAL == property.cardinality) {
//            property.cardinality = MCardinality.REQUIRED // just use nulled wrapper class
//        }
//    }
//    void setNotNull(MProperty property)
//    {
//        property.attr['notNull'] = MCardinality.REQUIRED == property.cardinality && !property.type.isPrimitive()
//    }
	MType schemaTypeToPropertyType(Type type, MCardinality container)
	{
		if (!type)
			throw new Error("Missing type")
		String typeName = schemaTypeToPropertyTypeName(type)
		MType javaType = MType.lookupType(typeName) //global type?
		if (!javaType) {
			visit(type) //nested type?
			javaType = nestedStack.peek().lookupClass(typeName)
		}
		if (!javaType)
			throw new Error("No type registed for ${typeName}")
		javaType
	}
	String schemaTypeToPropertyTypeName(Type type)
	{
		Type t = type
		while(t) {
			switch (t) {
				case SimpleType:
				case ComplexType:
					return classNameFunction.apply(t.qname.name)
				case TextOnlyType:
					if (mapToEnum(t)) {
						return enumClassNameFunction.apply(t.qname.name)
					} else if (t.isBuiltInType()) {
						return simpleXmlTypeToPropertyType.apply(t.qname.name)
					} else {
						t = t.base //go deeper looking for built-in type
					}
					break
				case List:
					t = t.itemType
					break
				case Union:
					t = (Type)t.simpleTypes[0] //try the first member
					break
				default:
					throw new IllegalStateException("not expecting ${t} converting type: ${type}")
			}
		}
		throw new IllegalStateException("TODO add support for converting type: ${type}")
	}

	void leftShift(MBase node) {
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
				nestedStack << node
				if (!isGlobalNode || !MType.lookupType(node.name)) {
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
	void rightShift(MBase node) {
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
