package com.javagen.gen.schema.node

import groovy.transform.ToString
import com.javagen.gen.schema.node.Value.NS
import com.javagen.gen.schema.node.Value.ProcessContents
import com.javagen.gen.schema.node.Value.Namespace

@ToString(includeSuper=true,includePackage=false)
class Any extends Element
{
    ProcessContents processContents = ProcessContents.strict
    NS namespace = Namespace.ANY
    @Override void setType(TextOnlyType type)
    {
        super.setType(type)
    }
}
