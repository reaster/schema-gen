package com.javagen.gen.schema

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

/** Qualified name has both a type name and a XML namespace */
@EqualsAndHashCode
//@ToString(includePackage=false,excludes='namespace')
class QName
{
    String name
    String namespace = 'http://www.w3.org/2001/XMLSchema'
    String toString() { name+":"+namespace }
}
