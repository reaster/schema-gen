package com.javagen.gen.schema.node

import com.javagen.gen.schema.QName
import groovy.transform.ToString

@ToString(includePackage=false)
trait AttributeHolder
{
    java.util.List<Attribute> attributes = []
    java.util.List<AttributeGroup> attributeGroups = []
    Attribute lookupAttribute(QName qname) { attributes.find { it.qname == qname} }
    AttributeGroup lookupAttributeGroup(QName qname) { attributeGroups.find { it.qname == qname} }
}
