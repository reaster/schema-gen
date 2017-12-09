package com.javagen.gen.schema.node

import com.javagen.gen.schema.QName
import groovy.transform.ToString

@ToString(includePackage=false)
trait ElementHolder
{
    java.util.List<Element> elements = []
    java.util.List<Group> groups = []
    Element lookupElement(QName qname) { elements.find { it.qname == qname} }
    Element lookupGroup(QName qname) { groups.find { it.qname == qname} }
}
