/*
 * Copyright (c) 2018 Outsource Cafe, Inc.
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

import com.javagen.schema.model.MAnnotation
import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.xml.XmlSchemaNormalizer
import com.javagen.schema.xml.node.Schema
import spock.lang.Specification

class FractionDigitsSpec extends Specification
{
    def "test decimal restrictions"() {
        given:
        def xml = """<?xml version="1.0" encoding="UTF-8"?>
<xsd:schema attributeFormDefault="unqualified"
            elementFormDefault="qualified"
            xmlns="http://fee.fi/fo" targetNamespace="http://fee.fi/fo" 
            xmlns:xsd="http://www.w3.org/2001/XMLSchema">
    <xsd:element name="envelope">
        <xsd:complexType>
            <xsd:sequence>
                <xsd:element name="nest_volume" nillable="true">
                    <xsd:simpleType>
                        <xsd:restriction base="xsd:decimal">
                            <xsd:totalDigits value="16"/>
                            <xsd:fractionDigits value="6"/>
                            <xsd:minInclusive value="0"/>
                        </xsd:restriction>
                    </xsd:simpleType>
                </xsd:element>
            </xsd:sequence>
        </xsd:complexType>
    </xsd:element>
</xsd:schema>"""
        JavaGen schemaVisitor = new JavaGen()
        when: "normalize schema and apply Java code generator"
        Schema schema = new XmlSchemaNormalizer().buildSchema(xml)
        schemaVisitor.visit(schema)
        MModule module = schemaVisitor.model
        then: "root module generated containing classes"
        module != null
        module.classes.size() > 0
        module.classes.each { println it }
        when: "ComplexType"
        MClass envClass = module.lookupClass('Envelope')
        then: "maps to Java Class with properties"
        envClass != null
        envClass.fields.size() > 0
        envClass.fields.values().each { println it }
        when: "nest_volume element with simpleType"
        MProperty nest_volume = envClass.fields['nestVolume']
        then: "maps to annotated property"
        nest_volume != null
        nest_volume.cardinality == MCardinality.REQUIRED
        nest_volume.getType().name == 'double'
        nest_volume.annotations.size() > 1
        MAnnotation min = nest_volume.annotations.find { it.expr.startsWith('@DecimalMin') }
        min?.toString() == '@DecimalMin("0")'
        MAnnotation digits = nest_volume.annotations.find { it.expr.startsWith('@Digits') }
        digits?.toString() == '@Digits(integer=10,fraction=6)'
    }

}
