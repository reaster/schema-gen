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

package com.javagen.schema.xml

import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
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
 * Allows fine-tunning of generated target language code. Subclasses may apply technology-specific annotations and other
 * implementation details. These methods are usualy the last thing called after the conversion (SchemaToJava,
 * SchemaToSwift, etc.) code has finished.
 *
 * TODO extends to support Union, List, Group, AttributeGroup, sequence, etc.
 *
 * @author Richard Easterling
 */
class XmlNodeCallback
{
    void gen(Schema schema, MModule module) { }
    void gen(Element element, MProperty property) {  }
    void gen(Attribute attribute, MProperty property) {  }
    void gen(AnyAttribute anyAttribute, MProperty property) {  }
    void gen(Any anyNode, MProperty property) {  }
    void gen(Any anyNode, MClass anyClass) { }
    void gen(Body body, MProperty property) {  }
    void gen(TextOnlyType textOnlyType, MEnum enumClass) {  }
    void gen(SimpleType simpleType, MClass clazz) {  }
    void gen(ComplexType complexType, MClass clazz) {  }
    void gen(Element element, MClass clazz) {  }
}
