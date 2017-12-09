package com.javagen.gen.kotlin

import com.javagen.gen.java.SchemaToJava
import com.javagen.gen.model.MCardinality
import com.javagen.gen.model.MClass
import com.javagen.gen.model.MEnum
import com.javagen.gen.model.MModule
import com.javagen.gen.model.MProperty
import com.javagen.gen.schema.XmlSchemaNormalizer
import com.javagen.gen.schema.node.Attribute
import com.javagen.gen.schema.node.ComplexType
import com.javagen.gen.schema.node.Schema
import com.javagen.gen.schema.node.SimpleType
import spock.lang.Shared

import static com.javagen.gen.schema.node.Schema.getDEFAULT_NS
import static com.javagen.gen.schema.node.Schema.getTargetNamespace

class SchemaToKotlinSpec {
    @Shared def prefixToNamespaceMap = ['xsd':DEFAULT_NS, 'gpx':'http://www.topografix.com/GPX/1/1', targetNamespace:'http://www.topografix.com/GPX/1/1']

}
