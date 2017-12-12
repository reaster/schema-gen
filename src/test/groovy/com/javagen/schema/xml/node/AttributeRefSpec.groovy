package com.javagen.schema.xml.node

import com.javagen.schema.java.SchemaToJava
import com.javagen.schema.model.MModule
import com.javagen.schema.xml.QName
import com.javagen.schema.xml.XmlSchemaNormalizer
import spock.lang.Specification

class AttributeRefSpec extends Specification
{
    def "test attribute mapping variations"() {
        given:
        //<!DOCTYPE xs:xml PUBLIC "-//W3C//DTD XMLSCHEMA 200102//EN" "../example-x-java/src/main/resources/XMLSchema.dtd" >

        def xml = """<?xml version='1.0'?>
<xs:xml targetNamespace="http://www.w3.org/XML/1998/namespace" xmlns:xs="http://www.w3.org/2001/XMLSchema" xml:lang="en">

 <xs:attribute name="lang" type="xs:language" />

 <xs:attribute name="space" default="preserve">
  <xs:simpleType>
   <xs:restriction base="xs:NCName">
    <xs:enumeration value="default"/>
    <xs:enumeration value="preserve"/>
   </xs:restriction>
  </xs:simpleType>
 </xs:attribute>

 <xs:attribute name="base" type="xs:anyURI" />

 <xs:attributeGroup name="specialAttrs">
  <xs:attribute ref="xml:base"/>
  <xs:attribute ref="xml:lang"/>
  <xs:attribute ref="xml:space"/>
 </xs:attributeGroup>

</xs:xml>
"""
        String ns = 'http://www.w3.org/XML/1998/namespace'
        SchemaToJava schemaVisitor = new SchemaToJava()
        when: "stage 1 - generate xml"
        Schema schema = new XmlSchemaNormalizer().buildSchema(xml)
        then: "normalized xml produced"
        schema != null
        when: "lang global atttribute defined"
        Attribute lang = schema.lookupAttribute(new QName(name:'lang', namespace:ns))
        then:
        lang != null
        lang.qname.name == 'lang'
        lang.type.qname.name == 'language'
        when: "base global atttribute defined"
        Attribute space = schema.lookupAttribute(new QName(name:'space', namespace:ns))
        then:
        space != null
        space.qname.name == 'space'
        space.type instanceof TextOnlyType
        space.type.restrictionSet().first() == Restriction.RType.enumeration
        space.type.restrictions.size() == 2
        space.'default' == 'preserve'
        when: "base global atttribute defined"
        Attribute base = schema.lookupAttribute(new QName(name:'base', namespace:ns))
        then:
        base != null
        base.qname.name == 'base'
        base.type.qname.name == 'anyURI'
        when: "xml AttributeGroups"
        Collection<AttributeGroup> ag = schema.globalAttributeGroups.values()
        then: "normalized ComplexType produced"
        ag.size() == 1
        when: "get specialAttrs"
        AttributeGroup specialAttrs = schema.globalAttributeGroups.get(new QName(name:'specialAttrs', namespace:ns))
        then:
        specialAttrs != null
        specialAttrs.attributes.size() == 3
        when: "stage 2 - generate object model from xml"
        schemaVisitor.visit(schema)
        MModule module = schemaVisitor.model
        then: "without elements, no classes generated"
        module != null
        module.classes.size() == 0
    }

}
