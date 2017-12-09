package com.javagen.gen.schema.node

import groovy.transform.ToString

//@ToString(includeSuper=true,includePackage=false)
class Attribute extends Value
{
    boolean required = false
    @Override String toString() { "<attribute @name='${qname?.name}' @type='${type?.qname?.name}' />"}
}
