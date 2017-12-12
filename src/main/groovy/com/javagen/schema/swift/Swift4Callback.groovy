package com.javagen.schema.swift

import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.xml.NodeCallback
import com.javagen.schema.xml.node.Any
import com.javagen.schema.xml.node.AnyAttribute
import com.javagen.schema.xml.node.Attribute
import com.javagen.schema.xml.node.Body
import com.javagen.schema.xml.node.ComplexType
import com.javagen.schema.xml.node.Element
import com.javagen.schema.xml.node.Schema
import com.javagen.schema.xml.node.SimpleType
import com.javagen.schema.xml.node.TextOnlyType

class Swift4Callback extends NodeCallback
{
    final SchemaToSwift gen
    final boolean validationAnnotations

    Swift4Callback(SchemaToSwift gen, boolean validationAnnotations = true)
    {
        this.gen = gen
        this.validationAnnotations = validationAnnotations
    }

    @Override void gen(Schema schema, MModule module) {

    }
    @Override void gen(Element element, MProperty property) {

    }
    @Override void gen(Attribute attribute, MProperty property) {

    }
    @Override void gen(AnyAttribute anyAttribute, MProperty property) {

    }

    @Override void gen(Any anyNode, MProperty property) {

    }
    @Override void gen(Body body, MProperty property) {

    }
    @Override void gen(TextOnlyType textOnlyType, MEnum enumClass)
    {
        enumClass.implements << 'String' << 'Codable'
    }
    @Override void gen(SimpleType simpleType, MClass clazz)
    {
        clazz.struct = gen.useStruct //use struct instead of class
        clazz.implements << 'Codable'
    }
    @Override void gen(ComplexType complexType, MClass clazz)
    {
        clazz.struct = gen.useStruct //use struct instead of class
        clazz.implements << 'Codable'
    }
    @Override void gen(Element element, MClass clazz) {

    }
}
