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
import com.javagen.schema.xml.XmlNodeCallback
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
 *
 * TODO use javax.annotation.Nullable and javax.annotation.Nonnull ?
 *
 * @author Richard Easterling
 */
class JavaJacksonCallback extends XmlNodeCallback
{
    final JavaGen gen
    final boolean validationAnnotations

    JavaJacksonCallback(JavaGen gen, boolean validationAnnotations = true)
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
            property.annotations << "@JacksonXmlElementWrapper(localName=\"${property.name}\"" + (element.type?.isWrapperElement() ? ')' : ', useWrapping=false)')
            property.annotations << "@JacksonXmlProperty(localName=\"${wrapperName}\")"
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper'
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        } else if (element.qname.name != property.name) {
            property.annotations << "@JacksonXmlProperty(localName=\"${element.qname.name}\")"
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        }
        applyRestrictions(property, element.type?.restrictions)
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
        if (body.element) {
            property.annotations << '@JacksonXmlCData'
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlCData'
        }
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
    /** root element callback */
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
        if (restrictions) {
            restrictions.each { restriction ->
                final String value = restriction.value
                if (value) {
                    switch (restriction.type) {
                        case minExclusive: //not really supported
                        case minInclusive:
                            if (JavaTypeRegistry.isFloatingPointType(field.type.name)) {
                                field.annotations << new MAnnotation(expr:"@DecimalMin(\"${value}\")", onGenericParam:true)
                                field.imports << 'javax.validation.constraints.DecimalMin'
                            } else {
                                field.annotations << new MAnnotation(expr:"@Min(${stripDecimals(value)})", onGenericParam:true)
                                field.imports << 'javax.validation.constraints.Min'
                            }
                            break
                        case maxExclusive: //not really supported
                        case maxInclusive:
                            if (JavaTypeRegistry.isFloatingPointType(field.type.name)) {
                                field.annotations << new MAnnotation(expr:"@DecimalMax(\"${value}\")", onGenericParam:true)
                                field.imports << 'javax.validation.constraints.DecimalMax'
                            } else {
                                field.annotations << new MAnnotation(expr:"@Max(${stripDecimals(value)})", onGenericParam:true)
                                field.imports << 'javax.validation.constraints.Max'
                            }
                            break
                        case whiteSpace: //preserve, replace, collapse
                            break
                        case pattern:
                            field.annotations << new MAnnotation(expr:"@Pattern(regexp=\"${escapeJavaRegexp(value)}\")", onGenericParam:true)
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
        }
        if (min && max) {
            field.annotations << "@Size(min=${min}, max=${max})"
            field.imports << 'javax.validation.constraints.Size'
        }
        if (fractionDigitsVal && totalDigitsVal) {
            String integerDigits = String.valueOf( Integer.parseInt(totalDigitsVal) - Integer.parseInt(fractionDigitsVal) )
            field.annotations << new MAnnotation(expr:"@Digits(integer=${integerDigits},fraction=${fractionDigitsVal}))", onGenericParam:true)
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

}
