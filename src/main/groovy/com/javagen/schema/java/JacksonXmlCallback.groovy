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

import com.javagen.schema.model.*
import com.javagen.schema.xml.NodeCallback
import com.javagen.schema.xml.node.Any
import com.javagen.schema.xml.node.AnyAttribute
import com.javagen.schema.xml.node.Attribute
import com.javagen.schema.xml.node.Body
import com.javagen.schema.xml.node.ComplexType
import com.javagen.schema.xml.node.Element
import com.javagen.schema.xml.node.Restriction
import com.javagen.schema.xml.node.TextOnlyType

import static com.javagen.schema.model.MMethod.Stereotype.getter
import static com.javagen.schema.model.MMethod.Stereotype.putter
import static com.javagen.schema.common.GlobalFunctionsUtil.*
import static com.javagen.schema.xml.node.Restriction.RType.*

/**
 * Decorate Java code with Jackson and Java validation constraint annotations.
 */
class JacksonXmlCallback extends NodeCallback
{
    final SchemaToJava gen
    final boolean validationAnnotations

    JacksonXmlCallback(SchemaToJava gen, boolean validationAnnotations = true)
    {
        this.gen = gen
        this.validationAnnotations = validationAnnotations
    }

    @Override void gen(Attribute attribute, MProperty property)
    {
        property.annotations << '@JacksonXmlProperty(isAttribute = true)'
        property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        applyRestrictions(property, attribute.type.restrictions)
    }
    void gen(AnyAttribute anyAttribute, MProperty property)
    {
        property.annotations << '@JacksonXmlProperty(isAttribute = true)'
        property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
    }
    @Override void gen(ComplexType complexType, MClass clazz)
    {
    }
    @Override void gen(Element element, MProperty property)
    {
        if (MCardinality.LIST == property.cardinality) {
            def wrapperName = gen.pluralService.toSingular(property.name)
            property.attr['singular'] = wrapperName
            property.annotations << "@JacksonXmlElementWrapper(localName=\"${property.name}\"" + (element.type.isWrapperElement() ? ')' : ', useWrapping=false)')
            property.annotations << "@JacksonXmlProperty(localName=\"${wrapperName}\")"
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper'
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        } else if (element.qname.name != property.name) {
            property.annotations << "@JacksonXmlProperty(localName=\"${element.qname.name}\")"
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        }
        applyRestrictions(property, element.type.restrictions)
    }
    @Override void gen(Any any, MProperty property)
    {
        if (property.methods[putter]) {
            property.methods[putter].annotations << '@JsonAnySetter'
            property.parent.imports << 'com.fasterxml.jackson.annotation.JsonAnySetter'
        }
        if (property.methods[getter]) {
            property.methods[getter].annotations << '@JsonAnyGetter'
            property.parent.imports << 'com.fasterxml.jackson.annotation.JsonAnyGetter'
       }
    }
    @Override void gen(Body body, MProperty property)
    {
        property.annotations << '@JacksonXmlText'
        property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText'
    }
    @Override void gen(TextOnlyType textOnlyType, MEnum enumClass)
    {
        enumClass.fields.values().each {
            if (it.name == gen.enumValueFieldName) {
                it.annotations << '@JsonValue'
                enumClass.imports << 'com.fasterxml.jackson.annotation.JsonValue'
            }
        }
    }
    @Override void gen(Element element, MClass clazz)
    {
        if (!clazz)
            throw new IllegalStateException("class can't be null for root element: ${element}")
        clazz.annotations << '@JsonIgnoreProperties(value = {"schemaLocation"})'
        clazz.imports << 'com.fasterxml.jackson.annotation.JsonIgnoreProperties'
    }

    private void applyRestrictions(MField field, List<Restriction> restrictions)
    {
        String min = null
        String max = null
        String fractionDigitsVal = null
        String totalDigitsVal = null
        restrictions.each { restriction ->
            final String value = restriction.value
            if (value) {
                switch (restriction.type) {
                    case minExclusive: //not really supported
                    case minInclusive:
                        if (JavaTypeRegistry.isFloatingPointType(field.type.name)) {
                            field.annotations << "@DecimalMin(\"${value}\")"
                            field.imports << 'javax.validation.constraints.DecimalMin'
                        } else {
                            field.annotations << "@Min(${stripDecimals(value)})"
                            field.imports << 'javax.validation.constraints.Min'
                        }
                        break
                    case maxExclusive: //not really supported
                    case maxInclusive:
                        if (JavaTypeRegistry.isFloatingPointType(field.type.name)) {
                            field.annotations << "@DecimalMax(\"${value}\")"
                            field.imports << 'javax.validation.constraints.DecimalMax'
                        } else {
                            field.annotations << "@Max(${stripDecimals(value)})"
                            field.imports << 'javax.validation.constraints.Max'
                        }
                        break
                    case whiteSpace: //preserve, replace, collapse
                        break
                    case pattern:
                        field.annotations << "@Pattern(regexp=\"${escapeJavaRegexp(value)}\")"
                        field.imports << 'javax.validation.constraints.Pattern'
                        break
                    case length:
                        min = max = stripDecimals(value)
                        break
                    case minLength:
                        min = stripDecimals(value)
                        break
                    case maxLength:
                        max = stripDecimals(value)
                        break
                    case fractionDigits:
                        fractionDigitsVal = val
                        break
                    case totalDigits:
                        totalDigitsVal = value
                        break
                    case enumeration: //handled elsewhere
                        break
                }
            }
        }
        if (min && max) {
            field.annotations << "@Size(min=${min}, max=${max})"
            field.imports << 'javax.validation.constraints.Size'
        }
        if (fractionDigitsVal && totalDigitsVal) {
            String integerDigits = String.valueOf( Integer.parseInt(totalDigitsVal) - Integer.parseInt(fractionDigitsVal) )
            field.annotations << "@Digits(integer=${integerDigits},fraction=${fractionDigitsVal}))"
            field.imports << 'javax.validation.constraints.Digits'
        }
        //boolean primitiveOrWrapper = JavaTypeRegistry.isPrimitive(field.type.name) || JavaTypeRegistry.isWrapper(field.type.name)
        //boolean notNull = MCardinality.REQUIRED == field.cardinality && !primitiveOrWrapper
        if (field.attr['notNull']) { //if useOptional==false, can't rely on REQUIRED because OPTIONALs are changed to REQUIRED
            field.annotations << "@NotNull"
            field.imports << 'javax.validation.constraints.NotNull'
        }
        boolean isRef = field.isReference()
        if (isRef) {
            field.annotations << "@Valid"
            field.imports << 'javax.validation.Valid'
        }
    }

//    private MField declareProperty(MClass clazz, MType nestedType, def propType, def propName, Container cardinality, String defaultVal, boolean isFinal)
//    {
//        assert clazz != null
//        MType type = nestedType ?: MType.lookupType(propType) // defaults to primitives
//        if (JavaTypeRegistry.containerRequiresPrimitiveWrapper(cardinality)) {
//            propType = JavaTypeRegistry.useWrapper(propType)
//            type = nestedType ?: MType.lookupType(propType)
//        }
//        if (!type)
//            type = clazz.lookupClass(propType) //try looking up nested type
//        if (!type)
//            throw new Error('No MType or MClass type registed for \"'+propType+'\" for '+clazz.name+'.'+propName)
//        boolean notNull = false
//        if (Container.OPTIONAL == cardinality) {
//            if (!xml.useOptional) {
//                cardinality = Container.REQUIRED // just use nulled wrapper class
//            }
//        } else if (Container.REQUIRED == cardinality && !type.isPrimitive()) {
//            notNull = true
//        }
//        boolean isReference = xml.isReferenceClass(propType)
//        MProperty prop = (isReference) ? new MReference(name:propName,type:type,cardinality:cardinality) : new MProperty(name:propName,type:type,cardinality:cardinality)
//        prop.setFinal(isFinal)
//        prop.setVal(defaultVal)
//        prop.attr['notNull'] = notNull
//        clazz.addField(prop)
//        return prop
//    }


}
