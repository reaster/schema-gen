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

import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.xml.XmlNodeCallback
import com.javagen.schema.xml.node.Any
import com.javagen.schema.xml.node.AnyAttribute
import com.javagen.schema.xml.node.Attribute
import com.javagen.schema.xml.node.Body
import com.javagen.schema.xml.node.ComplexType
import com.javagen.schema.xml.node.Element
import com.javagen.schema.xml.node.Schema
import com.javagen.schema.xml.node.SimpleType
import com.javagen.schema.xml.node.TextOnlyType

/**
 * Swap classes for structs, and decorates code with Swift's Codable protocol to support JSON marshalling.
 *
 * @author Richard Easterling
 */
class Swift4Callback extends XmlNodeCallback
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
