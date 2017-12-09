package com.javagen.gen.schema

import com.javagen.gen.model.MClass
import com.javagen.gen.model.MEnum
import com.javagen.gen.model.MModule
import com.javagen.gen.model.MProperty
import com.javagen.gen.schema.node.Any
import com.javagen.gen.schema.node.AnyAttribute
import com.javagen.gen.schema.node.Attribute
import com.javagen.gen.schema.node.Body
import com.javagen.gen.schema.node.ComplexType
import com.javagen.gen.schema.node.Element
import com.javagen.gen.schema.node.Schema
import com.javagen.gen.schema.node.SimpleType
import com.javagen.gen.schema.node.TextOnlyType

/**
 * Allows fine-tunning of generated target language code. Subclasses may apply technology-specific annotations and other
 * implementation details. These methods are usualy the last thing called after the conversion (SchemaToJava,
 * SchemaToSwift, etc.) code has finished.
 * TODO extends to support Union, List, Group, AttributeGroup, sequence, etc.
 */
class NodeCallback
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
