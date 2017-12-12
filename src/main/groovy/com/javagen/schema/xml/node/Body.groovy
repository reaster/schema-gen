package com.javagen.schema.xml.node

/** A Body element can be present in SimpleType and CompextType. It presents a challenge when mapping because it has
 * no name and may be of mixed content (text intermixed with child elements).
 */
class Body extends Value
{
    /** can be a SimpleType or ComplexType */
    SimpleType parent
    boolean mixedContent = false
}
