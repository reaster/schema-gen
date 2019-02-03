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

import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.common.GlobalFunctionsUtil
import com.javagen.schema.common.MappingUtil
import com.javagen.schema.common.PluralService
import com.javagen.schema.java.JavaGen
import com.javagen.schema.java.JavaPreEmitter
import com.javagen.schema.model.*
import com.javagen.schema.xml.XmlSchemaNormalizer
import com.javagen.schema.xml.node.Any
import com.javagen.schema.xml.node.Attribute
import com.javagen.schema.xml.node.Body
import com.javagen.schema.xml.node.Element
import com.javagen.schema.xml.node.TextOnlyType
import com.javagen.schema.xml.node.Type

import java.util.function.BiFunction
import java.util.function.Function

import static com.javagen.schema.common.GlobalFunctionsUtil.lowerCase
import static com.javagen.schema.common.GlobalFunctionsUtil.upperCase
import static com.javagen.schema.model.MMethod.IncludeProperties.allProperties
import static com.javagen.schema.model.MMethod.Stereotype.*
import static com.javagen.schema.xml.node.Schema.DEFAULT_NS

/**
 * TODO
 * Results -> List<HotSpring> results;, remove Results class
 * Media({}) removal
 *
 * Translate XML schema to Dart 2.1 code.
 *
 * <p>A XmlNodeCallback can be used to apply specific third-party library annotations to the object model, allowing one
 * to easily switch technologies. For example one could swap the DartToJsonCallback with a DartJaxbCallback without
 * having to rewrite the DartGen object model translation code.
 *
 * This class is the entry point for Dart code generation.
 *
 * @author Richard Easterling
 */
class DartGen extends JavaGen
{
    String sourceFileName = null

    @Override void optionalToPrimitiveWrapper(MProperty property) {}

    @Override def visit(Body body)
    {
        println "body @type=${body.type} @mixed=${body.mixedContent}"
        if (body.mixedContent) println "WARNING: mixed content currently not supported for body: ${body}"
        MCardinality container = container(body)
        String name = propertyNameFunction.apply(bodyPropertyName)
        String type = schemaTypeToPropertyType(body.type ?: schema.getGlobal(DEFAULT_NS, 'string'), container)

        MProperty property = new MProperty(name:name, type:type, scope: 'public', cardinality: container, attr: ['body':name])
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        //clazz.addMethod(new MMethod(stereotype: constructor, params:[new MBind(name: name, type: type)], body: "" ))
        callback.gen(body, property)
    }

    @Override MProperty genAny(String propertyName, Any any)
    {
        //println "any @name=${any.qname?.name}"
        MCardinality container = container(any)
        Type polymorphicType = polymporphicType(any) ?: any.type // any.type is not allowed in XML Schema?
        MType type = schemaTypeToPropertyType(polymorphicType ?: schema.getGlobal(DEFAULT_NS, anyType), container)
        MProperty property
        if (polymorphicType) {
            String val = any.fixed ?: any.'default' ?: (container == MCardinality.LIST) ? 'new java.util.ArrayList<>()' : null
            property = new MProperty(name:propertyName, type:type, cardinality:container, final:any.fixed!=null, val:val)
        } else if (container == MCardinality.LIST) {
            container = MCardinality.MAP
            MBind mapRType = new MBind(cardinality:container,type:type)
            String val = null //'LinkedHashMap()'
            property = new MProperty(name:propertyName, type:type, scope:'public', cardinality:container, final:any.fixed!=null, val:val, attr:['keyType':'String'])
        } else {
            String val = any.fixed ?: any.'default'
            property = new MProperty(name:propertyName, type:type, cardinality:container, final:any.fixed!=null, val:val)
        }
        setNotNull(property)
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        callback.gen(any, property)
        property
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
        MProperty property = new MProperty(name:name, type:type, scope:'public', cardinality:container, final:attribute.fixed!=null, val:val, restrictions:restrictions)
        setNotNull(property)
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        callback.gen(attribute, property)
    }

    @Override def visit(Element element)
    {
        MClass clazz = nestedStack.peek()
        TextOnlyType textOnlyType= clazz.attr['nodeType']
        if (clazz.name == 'Track') {
            println "element @name=${element.qname.name} @type=${element.type} -> ${clazz.name}"
        }
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
                    container = MCardinality.LIST
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
                throw new IllegalStateException("no type for element: ${element} with parent: ${textOnlyType}")
            }
        }
        String val = element.fixed ?: element.'default'
        java.util.List<MRestriction> restrictions = MappingUtil.translate(element)
        MProperty property = new MProperty(name:name, type:type, scope:'public', cardinality:container, final:element.fixed!=null, val:val, restrictions:restrictions)
        setNotNull(property)
        clazz.addField(property)
        callback.gen(element, property)
    }

    @Override MEnum generateEnumClass(MEnum enumClass)
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
        enumClass.enumValues = enumValues //.each { value -> enumValueFunction.apply(value) }
        enumClass
    }

    @Override String enumClassName(String tag)
    {
        int i = nestedStack.size()
        def clazz = i == 0 ? null : nestedStack.get(i-1)
        String name = enumClassNameFunction.apply(tag)
        while(clazz instanceof MClass) {
            name = "${clazz.name}_${name}"
            i--
            clazz = i == 0 ? null : nestedStack.get(i-1)
        }
        name
    }


    DartGen()
    {
        super(true)
        fileExtension = 'dart'
        useOptional = true //only applies to Java
        srcDir = new File('lib')
        anyType = 'anyType'
        simpleXmlTypeToPropertyType = { typeName ->
            DartTypeRegistry.simpleXmlTypeToPropertyType[typeName]
        }
        if ( ! MTypeRegistry.isInitialized() )
            new DartTypeRegistry()
        callback = new DartToJsonCallback(this, false)
        pipeline = [
                new DartPreEmitter(gen: this),
                new DartJsonEmitter(gen: this),
                new DartEmitter(gen: this)
        ]
        enumNameFunction = { text -> DartUtil.dartEnumName(text, false) }
        enumValueFunction = { text -> DartUtil.dartEnumValue(text, false) }
        propertyNameFunction = { text -> DartUtil.legalDartName(lowerCase(text)) }
        constantNameFunction = { text -> DartUtil.dartConstName(text) }
        classNameFunction = { text -> DartUtil.legalDartClassName(text) }

//        packageNameFunction = { ns -> packageName ?: ns ? GlobalFunctionsUtil.javaPackageFromNamespace(ns, true) : 'com.javagen.model' }
//        enumNameFunction = { text -> GlobalFunctionsUtil.javaEnumName(text, false) }
//        enumValueFunction = { text -> text }
//        enumClassNameFunction = { text -> GlobalFunctionsUtil.enumClassName(text, addSuffixToEnumClass) }
//        classNameFunction = { text -> GlobalFunctionsUtil.className(text, removeSuffixFromType) }
//        propertyNameFunction = { text -> GlobalFunctionsUtil.legalJavaName(lowerCase(text)) }
//        constantNameFunction = { text -> GlobalFunctionsUtil.javaConstName(text) }
//        collectionNameFunction = { singular -> customPluralMappings[singular] ?: pluralService.toPlural(singular) }
//        simpleXmlTypeToPropertyType
//        classOutputFileFunction = { gen, clazz -> new File(gen.srcDir, GlobalFunctionsUtil.pathFromPackage(clazz.fullName(),fileExtension))} //default works for Java
    }

    @Override def gen()
    {
        if (!customPluralMappings.isEmpty())
            pluralService = new PluralService(customPluralMappings) //pickup custom map
        schema = new XmlSchemaNormalizer().buildSchema(schemaURL)
        visit(schema)
        MModule rootModule = getModel()
        if (!sourceFileName) //if no source file name defined, use first root element name
            sourceFileName = schema.rootElements.isEmpty() ? 'javagen' : upperCase(schema.rootElements.first().name)
        sourceFileName = sourceFileName.toLowerCase()
        rootModule.sourceFile = pathFromSourceFileName(this, rootModule, sourceFileName)
        pipeline.each { visitor ->
            visitor.visit(rootModule)
        }
    }
}
