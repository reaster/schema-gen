package com.javagen.schema.xml.node

import groovy.transform.ToString

@ToString(includeNames = true,includeSuper=true,includePackage=false,ignoreNulls = true)
class Element extends Value
{
    /** true if no body, attributes or child elements - just used as a boolean tag */
    boolean empty = false
    boolean _abstract = false
    boolean nillable = false
    /** min allowed occurrences. Defaults to 1. */
    int minOccurs = 1
    /** max allowed occurrences. Defaults to 1. unbounded is converted to Integer.MAX */
    int maxOccurs = 1
    /** @return true if element contains text and/or child elements */
    boolean isBody() { type != null }
    boolean isMixed() { }
    void setAbstract(boolean _abstract) { this._abstract = _abstract }
    boolean isAbstract() { _abstract }
    boolean getAbstract(boolean _abstract) { _abstract }
}
