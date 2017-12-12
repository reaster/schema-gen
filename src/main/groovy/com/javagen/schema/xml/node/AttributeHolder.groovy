package com.javagen.schema.xml.node

import com.javagen.schema.xml.QName
import groovy.transform.ToString

@ToString(includePackage=false)
trait AttributeHolder
{
    java.util.List<Attribute> attributes = []
    java.util.List<AttributeGroup> attributeGroups = []
    Attribute lookupAttribute(QName qname) { attributes.find { it.qname == qname} }
    AttributeGroup lookupAttributeGroup(QName qname) { attributeGroups.find { it.qname == qname} }
}
