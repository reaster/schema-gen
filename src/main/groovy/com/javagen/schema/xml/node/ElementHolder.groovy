package com.javagen.schema.xml.node

import com.javagen.schema.xml.QName
import groovy.transform.ToString

@ToString(includePackage=false)
trait ElementHolder
{
    java.util.List<Element> elements = []
    java.util.List<Group> groups = []
    Element lookupElement(QName qname) { elements.find { it.qname == qname} }
    Element lookupGroup(QName qname) { groups.find { it.qname == qname} }
}
