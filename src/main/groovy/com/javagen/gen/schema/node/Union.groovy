package com.javagen.gen.schema.node

import groovy.transform.ToString

/**
 * Defines a collection of multiple simpleType definitions.
 */
@ToString(includeSuper=true,includePackage=false)
class Union extends Type
{
    java.util.List<TextOnlyType> simpleTypes = []
}
