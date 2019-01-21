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

package com.javagen.schema.kotlin

import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.common.PluralService
import com.javagen.schema.java.JavaGen
import com.javagen.schema.model.MBind
import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MMethod
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.model.MReference
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry
import com.javagen.schema.xml.XmlSchemaNormalizer
import com.javagen.schema.xml.node.Any
import com.javagen.schema.xml.node.Body

import static com.javagen.schema.model.MMethod.Stereotype.constructor
import static com.javagen.schema.model.MMethod.Stereotype.equals
import static com.javagen.schema.model.MMethod.Stereotype.getter
import static com.javagen.schema.model.MMethod.Stereotype.hash
import static com.javagen.schema.model.MMethod.Stereotype.setter
import static com.javagen.schema.model.MMethod.Stereotype.toString
import static com.javagen.schema.xml.node.Schema.DEFAULT_NS
import static com.javagen.schema.common.GlobalFunctionsUtil.lowerCase
import static com.javagen.schema.common.GlobalFunctionsUtil.upperCase

/**
 * Translate XML schema to Kotlin code.
 *
 * <p>A XmlNodeCallback can be used to apply specific third-party library annotations to the object model, allowing one
 * to easily switch technologies. For example one could swap the KatlinToJsonCallback with a KotlinJaxbCallback without
 * having to rewrite the KatlinGen object model translation code.
 *
 * This class is the entry point for Kotlin code generation.
 *
 * @author Richard Easterling
 */
class KotlinGen extends JavaGen
{
    String sourceFileName = null

    @Override void optionalToPrimitiveWrapper(MProperty property) {}
    @Override MEnum javaEnum(MEnum enumClass)
    {
        List<String> enumValues = enumClass.enumValues.sort()
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
        enumClass.addField( new MProperty(name: enumValueFieldName, scope: 'private', 'final': true) )
        enumClass
    }
    @Override def visit(Body body)
    {
        //println "body @type=${body.type} @mixed=${body.mixedContent}"
        if (body.mixedContent) println "WARNING: mixed content currently not supported for body: ${body}"
        MCardinality container = container(body)
        String name = propertyNameFunction.apply(bodyPropertyName)
        String type = schemaTypeToPropertyType(body.type ?: schema.getGlobal(DEFAULT_NS, 'string'), container)

        MProperty property = new MProperty(name:name, type:type, cardinality: container, attr: ['body':name])
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        clazz.addMethod(new MMethod(stereotype: constructor, params:[new MBind(name: name, type: type)], body: "" ))
        callback.gen(body, property)
    }

    @Override MProperty genAny(String propertyName, Any any)
    {
        MClass clazz = nestedStack.peek()
        MCardinality container = container(any)
        MType type = schemaTypeToPropertyType(any.type ?: schema.getGlobal(DEFAULT_NS, anyType), container)
        MProperty property
        if (container == MCardinality.LIST) {
            /*
            class Foo(@JsonIgnore var map:LinkedHashMap<String, String> = linkedMapOf())
            {
                public @JsonAnySetter fun set(key: String, value: String) = this.map.put(key, value)
                public @JsonAnyGetter fun all(): Map<String, String> = this.map
                override fun hashCode() = map.hashCode()
                override fun equals(other: Any?) = when {
                    this === other -> true
                    other is Foo -> map.size == other.map.size
                                            && map.all { (k,v) -> v.equals(other.map[k]) }
                    else -> false
                }
                override fun toString() = map.toString()
            }
             */
//            if (propertyName == 'any')
//                println("any")
            String anyClassName = classNameFunction.apply(propertyName)
            MClass anyClass = (MClass)MType.lookupType(anyClassName)
            if (!anyClass || anyClass.fields.isEmpty()) { //TODO should this be done in the ComplexType method?
                anyClass = new MClass(name: anyClassName)
                MBind keyParam = new MBind(name: 'key', type: 'String')
                MBind valParam = new MBind(name: 'value', type: type)
                MBind anyParam = new MBind(name: 'other', type: 'Any', cardinality: MCardinality.OPTIONAL)
                MProperty anyProp = new MProperty(name:'map', type:type, cardinality:MCardinality.LINKEDMAP, val:'linkedMapOf()', attr:['keyType':'String'])
                anyClass.addField( anyProp )
                anyClass.addMethod(new MMethod(name: 'set', params: [keyParam, valParam], expr: 'this.map.put(key, value)', stereotype: setter))
                anyClass.addMethod(new MMethod(name: 'all', type: anyProp, expr: 'this.map', stereotype: getter))
                anyClass.addMethod(new MMethod(name: 'hashCode', override: true, expr: 'map.hashCode()', stereotype: hash))
                anyClass.addMethod(new MMethod(name: 'equals', override: true, params: [anyParam], expr: this.&mapHashCodeMethodBody, stereotype: equals))
                anyClass.addMethod(new MMethod(name: 'toString', override: true, expr: 'map.toString()', stereotype: toString))
                clazz.parentModule().addClass(anyClass)
                MType.registerType(anyClass)
                callback.gen(any, anyClass)
            }
            property = new MReference(name:propertyName, type:anyClass, cardinality:MCardinality.OPTIONAL, final:any.fixed!=null)

        } else {
            String val = any.fixed ?: any.'default'
            property = new MProperty(name:propertyName, type:type, cardinality:container, final:any.fixed!=null, val:val)
        }
        clazz.addField(property)
        callback.gen(any, property)
        property
    }

    def mapHashCodeMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        v.out << 'when {'
        v.next()
        v.out << '\n' << v.tabs << 'this === other -> true'
        v.out << '\n' << v.tabs << "other is ${m.parent.name} -> map.size == other.map.size"
        v.next()
        v.out << '\n' << v.tabs << '&& map.all { (k,v) -> v.equals(other.map[k]) }'
        v.previous()
        v.out << '\n' << v.tabs << 'else -> false'
        v.previous()
        v.out << '\n' << v.tabs << '}'
    }

    KotlinGen()
    {
        super(true)
        fileExtension = 'kt'
        useOptional = true //only applies to Java
        srcDir = new File('src/main/kotlin-gen')
        simpleXmlTypeToPropertyType = { typeName ->
            KotlinTypeRegistry.simpleXmlTypeToPropertyType[typeName]
        }
        if ( ! MTypeRegistry.isInitialized() )
            new KotlinTypeRegistry()
        callback = new KotlinJacksonCallback(this)
        pipeline = [
                new KotlinEmitter(gen: this)
        ]
        enumNameFunction = { text -> KotlinUtil.kotlinEnumName(text, false) }
        propertyNameFunction = { text -> KotlinUtil.legalKotlinName(lowerCase(text)) }
        constantNameFunction = { text -> KotlinUtil.kotlinConstName(text) }
        classNameFunction = { text -> KotlinUtil.legalKotlinClassName(text) }
    }

    @Override def gen()
    {
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
}
