package com.javagen.gen.swift

import com.javagen.gen.Gen
import com.javagen.gen.schema.XmlSchemaNormalizer
import com.javagen.gen.model.MBase
import com.javagen.gen.model.MBind
import com.javagen.gen.model.MCardinality
import com.javagen.gen.model.MClass
import com.javagen.gen.model.MEnum
import com.javagen.gen.model.MField
import com.javagen.gen.model.MMethod
import com.javagen.gen.model.MModule
import com.javagen.gen.model.MSource
import com.javagen.gen.model.MProperty
import com.javagen.gen.model.MReference
import com.javagen.gen.model.MType
import com.javagen.gen.model.MTypeRegistry
import com.javagen.gen.schema.NodeCallback
import com.javagen.gen.schema.QName
import com.javagen.gen.schema.SchemaVisitor
import com.javagen.gen.schema.node.Any
import com.javagen.gen.schema.node.AnyAttribute
import com.javagen.gen.schema.node.Attribute
import com.javagen.gen.schema.node.AttributeGroup
import com.javagen.gen.schema.node.Body
import com.javagen.gen.schema.node.ComplexType
import com.javagen.gen.schema.node.Element
import com.javagen.gen.schema.node.Group
import com.javagen.gen.schema.node.List
import com.javagen.gen.schema.node.Restriction
import com.javagen.gen.schema.node.Schema
import com.javagen.gen.schema.node.SimpleType
import com.javagen.gen.schema.node.TextOnlyType
import com.javagen.gen.schema.node.Type
import com.javagen.gen.schema.node.Union
import com.javagen.gen.util.GlobalFunctionsUtil

import java.util.function.BiFunction

//import static com.javagen.gen.java.JavaTypeRegistry.containerRequiresPrimitiveWrapper
//import static com.javagen.gen.java.JavaTypeRegistry.useWrapper
import static com.javagen.gen.model.MCardinality.LIST
import static com.javagen.gen.schema.node.Schema.DEFAULT_NS
import static com.javagen.gen.util.GlobalFunctionsUtil.lowerCase
import static com.javagen.gen.util.GlobalFunctionsUtil.upperCase

class SchemaToSwift extends Gen implements SchemaVisitor
{
    URL schemaFile
    NodeCallback callback

    Map<String,MModule> moduleMap = new LinkedHashMap<>()
    Map<String,MClass> classMap = [:]
    Stack<MSource> nestedStack = []
    MModule rootModule
    static final Set<String> CONTAINER_TYPES = ['NMTOKENS','IDREFS','ENTITIES'] as Set
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


    SchemaToSwift()
    {
        super()
        if ( ! MTypeRegistry.isInitialized() )
            new SwiftTypeRegistry()
        callback = new Swift4Callback(this)
        pipeline = [
                new SwiftPreEmitter(gen: this),
                new SwiftEmitter(gen: this)
        ]
        fileExtension = 'swift'
        simpleXmlTypeToPropertyType = { typeName -> SwiftTypeRegistry.simpleXmlTypeToPropertyType[typeName] }
        classOutputFile = { gen,clazz -> Gen.fileNmeFromAttr(gen, clazz) } //combine classes into single source file
        enumNameFunction = { text -> GlobalFunctionsUtil.swiftEnumName(text, false) }
        propertyNameFunction = { text -> GlobalFunctionsUtil.legalSwiftName(lowerCase(text)) }
        constantNameFunction = { text -> GlobalFunctionsUtil.swiftConstName(text) }
    }

    @Override def gen()
    {
        schema = new XmlSchemaNormalizer().buildSchema(schemaFile)
        visit(schema)
        if (!sourceFileName) //if no source file name defined, use first root element name
            sourceFileName = schema.rootElements.isEmpty() ? 'JavaGen' : upperCase(schema.rootElements.first().name)
        rootModule.attr['fileName'] = sourceFileName
        super.gen()
    }

    @Override
    MModule getModel()
    {
        return rootModule
    }

    def visit(Schema schema)
    {
        this.schema = schema
        String name = packageNameFunction.apply(schema.prefixToNamespaceMap[Schema.targetNamespace])
        rootModule = new MModule(name:name)
        this << rootModule
        SchemaVisitor.super.preVisit(schema) //visit global elements, pre-create classes for type reference lookups
        SchemaVisitor.super.visit(schema)
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
        this << clazz
        SchemaVisitor.super.visit(complexType)
        this >> clazz
        clazz.ignore = complexType.isWrapperElement()
        callback.gen(complexType, clazz)
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
        SchemaVisitor.super.visit(simpleType)
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
        println "root node: ${root}"
        Element rootElement = schema.lookupElement(root)
        String className = classNameFunction.apply(rootElement.type.qname.name)
        MClass clazz = rootModule.lookupClass(className)
        callback.gen(rootElement, clazz)
    }

    ////////////////////////////////////////////////////////////////////////////
    // gen methods
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
        if (type.qname?.name == 'anyURI') {
            println 'TODO remvoe me'
        }
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
        String javaType
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
                    t = t.simpleTypes[0] //try the first member
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
