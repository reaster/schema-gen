package com.javagen.gen.kotlin

import com.javagen.gen.java.JavaTypeRegistry
import com.javagen.gen.model.MCardinality
import com.javagen.gen.model.MClass
import com.javagen.gen.model.MEnum
import com.javagen.gen.model.MField
import com.javagen.gen.model.MMethod
import com.javagen.gen.model.MProperty
import com.javagen.gen.schema.NodeCallback
import com.javagen.gen.schema.node.Any
import com.javagen.gen.schema.node.AnyAttribute
import com.javagen.gen.schema.node.Attribute
import com.javagen.gen.schema.node.Body
import com.javagen.gen.schema.node.ComplexType
import com.javagen.gen.schema.node.Element
import com.javagen.gen.schema.node.Restriction
import com.javagen.gen.schema.node.SimpleType
import com.javagen.gen.schema.node.TextOnlyType

import static com.javagen.gen.schema.node.Restriction.RType.enumeration
import static com.javagen.gen.schema.node.Restriction.RType.fractionDigits
import static com.javagen.gen.schema.node.Restriction.RType.length
import static com.javagen.gen.schema.node.Restriction.RType.maxExclusive
import static com.javagen.gen.schema.node.Restriction.RType.maxInclusive
import static com.javagen.gen.schema.node.Restriction.RType.maxLength
import static com.javagen.gen.schema.node.Restriction.RType.minExclusive
import static com.javagen.gen.schema.node.Restriction.RType.minInclusive
import static com.javagen.gen.schema.node.Restriction.RType.minLength
import static com.javagen.gen.schema.node.Restriction.RType.pattern
import static com.javagen.gen.schema.node.Restriction.RType.totalDigits
import static com.javagen.gen.schema.node.Restriction.RType.whiteSpace
import static com.javagen.gen.util.GlobalFunctionsUtil.escapeJavaRegexp
import static com.javagen.gen.util.GlobalFunctionsUtil.stripDecimals


/**
 * Decorate Kotlin code with Jackson and validation constraint annotations.
 */
class KotlinJacksonCallback extends NodeCallback
{
    final KotlinGen gen
    final boolean validationAnnotations

    KotlinJacksonCallback(KotlinGen gen, boolean validationAnnotations = true)
    {
        this.gen = gen
        this.validationAnnotations = validationAnnotations
    }
    @Override void gen(SimpleType simpleType, MClass clazz)
    {
        clazz.data = true
    }
    @Override void gen(ComplexType complexType, MClass clazz)
    {
        clazz.data = true
    }
    @Override void gen(Element element, MClass clazz)
    {
        if (!clazz)
            throw new IllegalStateException("class can't be null for root element: ${element}")
        clazz.annotations << '@JsonIgnoreProperties(value = ["schemaLocation"])'
        clazz.imports << 'com.fasterxml.jackson.annotation.JsonIgnoreProperties'
        clazz.data = true
    }

    @Override void gen(Attribute attribute, MProperty property)
    {
        property.annotations << "@JacksonXmlProperty(localName=\"${property.name}\",isAttribute = true)"
        //property.annotations << "@JacksonXmlProperty(isAttribute = true)"
        property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        applyRestrictions(property, attribute.type.restrictions)
    }
    void gen(AnyAttribute anyAttribute, MProperty property)
    {
        property.annotations << "@JacksonXmlProperty(isAttribute = true)"
        property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
    }
    @Override void gen(Element element, MProperty property)
    {
        if (MCardinality.LIST == property.cardinality) {
            def wrapperName = gen.pluralService.toSingular(property.name)
            property.attr['singular'] = wrapperName
            property.annotations << "@JacksonXmlElementWrapper(localName=\"${property.name}\"" + (element.type.isWrapperElement() ? ')' : ', useWrapping=false)')
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper'
            property.annotations << "@JacksonXmlProperty(localName=\"${wrapperName}\")"
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        } else if (element.qname.name != property.name) {
            //property.annotations << "@JacksonXmlProperty(localName=\"${element.qname.name}\")"
            //property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        } else {
            //property.annotations << "@JacksonXmlProperty(localName=\"${element.qname.name}\")"
            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
        }
        applyRestrictions(property, element.type.restrictions)
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

    void gen(Any anyNode, MProperty property)
    {

    }
    void gen(Any anyNode, MClass anyClass)
    {
        anyClass.fields.values().each { MField f ->
            f.annotations << '@JsonIgnore'
            anyClass.imports << 'com.fasterxml.jackson.annotation.JsonIgnore'
        }
        MMethod m = anyClass.findMethod(MMethod.Stereotype.setter)
        if (m) {
            m.annotations << '@JsonAnySetter'
            anyClass.imports << 'com.fasterxml.jackson.annotation.JsonAnySetter'
        }
        m = anyClass.findMethod(MMethod.Stereotype.getter)
        if (m) {
            m.annotations << '@JsonAnyGetter'
            anyClass.imports << 'com.fasterxml.jackson.annotation.JsonAnyGetter'
        }
    }


    private void applyRestrictions(MField field, java.util.List<Restriction> restrictions) {}
}

/*
    @Override void gen(Attribute attribute, MProperty property)
    {
//        property.annotations << '@JacksonXmlProperty(isAttribute = true)'
//        property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
//        applyRestrictions(property, attribute.type.restrictions)
    }
    void gen(AnyAttribute anyAttribute, MProperty property)
    {
//        property.annotations << '@JacksonXmlProperty(isAttribute = true)'
//        property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
    }
    @Override void gen(Element element, MProperty property)
    {
//        if (MCardinality.LIST == property.cardinality) {
//            def wrapperName = gen.pluralService.toSingular(property.name)
//            property.attr['singular'] = wrapperName
//            property.annotations << "@JacksonXmlElementWrapper(localName=\"${property.name}\"" + (element.type.isWrapperElement() ? ')' : ', useWrapping=false)')
//            property.annotations << "@JacksonXmlProperty(localName=\"${wrapperName}\")"
//            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper'
//            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
//        } else if (element.qname.name != property.name) {
//            property.annotations << "@JacksonXmlProperty(localName=\"${element.qname.name}\")"
//            property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty'
//        }
        //applyRestrictions(property, element.type.restrictions)
    }
    @Override void gen(Any any, MProperty property)
    {
//        if (property.methods[putter]) {
//            property.methods[putter].annotations << '@JsonAnySetter'
//            property.parent.imports << 'com.fasterxml.jackson.annotation.JsonAnySetter'
//        }
//        if (property.methods[getter]) {
//            property.methods[getter].annotations << '@JsonAnyGetter'
//            property.parent.imports << 'com.fasterxml.jackson.annotation.JsonAnyGetter'
    }
}
@Override void gen(Body body, MProperty property)
{
//        property.annotations << '@JacksonXmlText'
//        property.parent.imports << 'com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText'
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

private void applyRestrictions(MField field, java.util.List<Restriction> restrictions)
{
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
        String integerDigits = String.valueOf( Integer.parseInt(totalDigitsVal) - Integer.parseInt(fractionDigitsVal) );
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

 */