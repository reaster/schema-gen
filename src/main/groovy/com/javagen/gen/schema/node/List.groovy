package com.javagen.gen.schema.node

import groovy.transform.ToString

@ToString(includeSuper=true,includePackage=false)
class List extends Type
{
    void setItemType(Type itemType) { this.base = itemType }
    Type getItemType() { this.base }
}
