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

import com.javagen.schema.java.JavaTypeRegistry
import com.javagen.schema.model.*
import com.javagen.schema.xml.XmlNodeCallback
import com.javagen.schema.xml.node.*

import static com.javagen.schema.common.GlobalFunctionsUtil.escapeJavaRegexp
import static com.javagen.schema.common.GlobalFunctionsUtil.stripDecimals
import static com.javagen.schema.xml.node.Restriction.RType.*

/**
 * Decorate Dart code with json_annotation metadata.
 *
 * @author Richard Easterling
 */
class DartToJsonCallback extends XmlNodeCallback {
    final DartGen gen
    final boolean validationAnnotations
    final boolean generateNullable

    DartToJsonCallback(DartGen gen, boolean validationAnnotations = true, boolean generateNullable = true) {
        this.gen = gen
        this.validationAnnotations = validationAnnotations
        this.generateNullable = generateNullable
    }

    @Override
    void gen(SimpleType simpleType, MClass clazz) {
        //clazz.annotations << '@JsonSerializable(nullable: false)'
        clazz.annotations << '@JsonSerializable()'
        clazz.imports << 'json_annotation/json_annotation.dart'
        clazz.parts << "${gen.sourceFileName}.g.${gen.fileExtension}"
    }

    @Override
    void gen(ComplexType complexType, MClass clazz) {
        clazz.annotations << '@JsonSerializable()'
        clazz.imports << 'json_annotation/json_annotation.dart'
        clazz.parts << "${gen.sourceFileName}.g.${gen.fileExtension}"
    }

    @Override
    void gen(Element element, MClass clazz) {
        if (!clazz)
            throw new IllegalStateException("class can't be null for root element: ${element}")
//        clazz.annotations << '@JsonSerializable()'
//        clazz.imports << 'json_annotation/json_annotation.dart'
//        clazz.parts << "${gen.sourceFileName}.g.${gen.fileExtension}"
        //clazz.data = true
    }

    @Override
    void gen(Attribute attribute, MProperty property) {
        def name = attribute.qname.name != property.name ? attribute.qname.name : null
        def nullable = generateNullable && property.isOptional() ? 'true' : null
        if (name || nullable) {
            def nameAttr = name ? "name: \"${name}\"" : ''
            def comma = (name && nullable) ? ', ' : ''
            def nullableAttr = nullable ? 'nullable: true' : ''
            property.annotations << "@JsonKey(${nameAttr}${comma}${nullableAttr})"
        }
        //property.annotations << "@JacksonXmlProperty(localName=\"${property.name}\",isAttribute = true)"
        //property.annotations << "@JacksonXmlProperty(isAttribute = true)"
        //property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        applyRestrictions(property, attribute.type.restrictions)
    }

    void gen(AnyAttribute anyAttribute, MProperty property) {
        //property.annotations << "@JacksonXmlProperty(isAttribute = true)"
        //property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
    }

    @Override
    void gen(Element element, MProperty property) {
        if (MCardinality.LIST == property.cardinality) {
            def wrapperName = gen.pluralService.toSingular(property.name)
            property.attr['singular'] = wrapperName
            //property.annotations << "@JacksonXmlElementWrapper(localName=\"${property.name}\"" + (element.type.isWrapperElement() ? ')' : ', useWrapping=false)')
            //property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper'
            //property.annotations << "@JacksonXmlProperty(localName=\"${wrapperName}\")"
            //property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        } else if (element.qname.name != property.name) {
            def nullableAttr = generateNullable && property.isOptional() ? ', nullable: true' : ''
            property.annotations << "@JsonKey(name: \"${name}\"${nullableAttr})"
            //property.annotations << "@JacksonXmlProperty(localName=\"${element.qname.name}\")"
            //property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        } else {
            //property.annotations << "@JacksonXmlProperty(localName=\"${element.qname.name}\")"
            //property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        }
        applyRestrictions(property, element.type.restrictions)
    }

    @Override
    void gen(Body body, MProperty property) {
        //property.annotations << '@JacksonXmlText'
        //property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText'
    }

    @Override
    void gen(TextOnlyType textOnlyType, MEnum enumClass) {
        boolean genImport = false
        enumClass.enumNames.eachWithIndex { String name, int i ->
            if (name != enumClass.enumValues[i]) {
                enumClass.annotationValues[i] =  "@JsonKey(name: \"${gen.enumValueFunction.apply(enumClass.enumValues[i])}\")"
                genImport = true
            } else {
                enumClass.annotationValues[i] = ''
            }
            if (genImport)
                enumClass.imports << 'json_annotation/json_annotation.dart'
        }
//            def name = attribute.qname.name != property.name ? attribute.qname.name : null
//            if (it.name == gen.enumValueFieldName) {
//                //it.annotations << '@JsonValue'
//                //enumClass.imports << 'com.fasterxml.jackson.annotation.JsonValue'
//            }
//        }
    }

    void gen(Any anyNode, MProperty property) {
        applyRestrictions(property)
    }

    void gen(Any anyNode, MClass anyClass) {
        anyClass.fields.values().each { MField f ->
            //f.annotations << '@JsonIgnore'
            //anyClass.imports << 'com.fasterxml.jackson.annotation.JsonIgnore'
        }
        MMethod m = anyClass.findMethod(MMethod.Stereotype.setter)
        if (m) {
            //m.annotations << '@JsonAnySetter'
            //anyClass.imports << 'com.fasterxml.jackson.annotation.JsonAnySetter'
        }
        m = anyClass.findMethod(MMethod.Stereotype.getter)
        if (m) {
            //m.annotations << '@JsonAnyGetter'
            //anyClass.imports << 'com.fasterxml.jackson.annotation.JsonAnyGetter'
        }
    }

    //private void applyRestrictions(MField field, List<Restriction> restrictions) {}
    private void applyRestrictions(MField field, java.util.List<Restriction> restrictions = []) {
        if (!validationAnnotations)
            return
        String min = null;
        String max = null;
        String fractionDigitsVal = null;
        String totalDigitsVal = null
        restrictions.each { restriction ->
            final String value = restriction.value
            if (value) {
                switch (restriction.type) {
                    case minExclusive: //not really supported
                    case minInclusive:
                        if (JavaTypeRegistry.isFloatingPointType(field.type.name)) {
                            field.annotations << "@get:DecimalMin(\"${value}\")"
                            field.imports << 'javax.validation.constraints.DecimalMin'
                        } else {
                            field.annotations << "@get:Min(${stripDecimals(value)})"
                            field.imports << 'javax.validation.constraints.Min'
                        }
                        break
                    case maxExclusive: //not really supported
                    case maxInclusive:
                        if (JavaTypeRegistry.isFloatingPointType(field.type.name)) {
                            field.annotations << "@get:DecimalMax(\"${value}\")"
                            field.imports << 'javax.validation.constraints.DecimalMax'
                        } else {
                            field.annotations << "@get:Max(${stripDecimals(value)})"
                            field.imports << 'javax.validation.constraints.Max'
                        }
                        break
                    case whiteSpace: //preserve, replace, collapse
                        break
                    case pattern:
                        field.annotations << "@get:Pattern(regexp=\"${escapeJavaRegexp(value)}\")"
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
                        fractionDigitsVal = value
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
            field.annotations << "@get:Size(min=${min}, max=${max})"
            field.imports << 'javax.validation.constraints.Size'
        }
        if (fractionDigitsVal && totalDigitsVal) {
            String integerDigits = String.valueOf(Integer.parseInt(totalDigitsVal) - Integer.parseInt(fractionDigitsVal));
            field.annotations << "@get:Digits(integer=${integerDigits},fraction=${fractionDigitsVal}))"
            field.imports << 'javax.validation.constraints.Digits'
        }
        //boolean primitiveOrWrapper = JavaTypeRegistry.isPrimitive(field.type.name) || JavaTypeRegistry.isWrapper(field.type.name)
        //boolean notNull = MCardinality.REQUIRED == field.cardinality && !primitiveOrWrapper
        //if (field.attr['notNull']) {
        //    //if useOptional==false, can't rely on REQUIRED because OPTIONALs are changed to REQUIRED
        //    field.annotations << "@get:NotNull"
        //    field.imports << 'javax.validation.constraints.NotNull'
        //}
        boolean isRef = field.isReference()
        if (isRef) {
            field.annotations << "@field:Valid"
            field.imports << 'javax.validation.Valid'
        }

    }
}
