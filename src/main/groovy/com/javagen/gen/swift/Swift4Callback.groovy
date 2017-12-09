package com.javagen.gen.swift

import com.javagen.gen.model.MCardinality
import com.javagen.gen.model.MClass
import com.javagen.gen.model.MEnum
import com.javagen.gen.model.MModule
import com.javagen.gen.model.MProperty
import com.javagen.gen.model.MReference
import com.javagen.gen.model.MType
import com.javagen.gen.schema.NodeCallback
import com.javagen.gen.schema.node.Any
import com.javagen.gen.schema.node.AnyAttribute
import com.javagen.gen.schema.node.Attribute
import com.javagen.gen.schema.node.Body
import com.javagen.gen.schema.node.ComplexType
import com.javagen.gen.schema.node.Element
import com.javagen.gen.schema.node.Schema
import com.javagen.gen.schema.node.SimpleType
import com.javagen.gen.schema.node.TextOnlyType

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
