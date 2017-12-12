package com.javagen.schema.xml

import groovy.transform.EqualsAndHashCode

/** Qualified name has both a type name and a XML namespace */
@EqualsAndHashCode
//@ToString(includePackage=false,excludes='namespace')
class QName
{
    String name
    String namespace = 'http://www.w3.org/2001/XMLSchema'
    String toString() { name+":"+namespace }
}
