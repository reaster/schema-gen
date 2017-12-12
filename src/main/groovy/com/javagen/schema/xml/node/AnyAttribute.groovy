package com.javagen.schema.xml.node

import groovy.transform.ToString
import com.javagen.schema.xml.node.Value.NS
import com.javagen.schema.xml.node.Value.ProcessContents
import com.javagen.schema.xml.node.Value.Namespace

@ToString(includeSuper=true,includePackage=false)
class AnyAttribute extends Attribute
{
    ProcessContents processContents = ProcessContents.strict
    NS namespace = Namespace.ANY
}
