package com.javagen.gen.schema.node

import groovy.transform.ToString

/**
 * SimpleTypes contain only text content (a Body) and attributes. SimpleTypes are generlly mapped to a MClass.
 */
//@ToString(includeSuper=true,includePackage=false)
class SimpleType extends TextOnlyType implements AttributeHolder
{
    boolean mixedContent = false
    /** signals an empty element. TODO not sure this is implemented correctly */
    boolean isEmpty() { base == null }
    /** @return true if contains a single value, text only content. */
    @Override boolean isSimpleType() { false }
    /** @return true if contains text only content and attributes. */
    @Override boolean isSimpleContent() { true }
    /** @return true if contains child elements and attributes. */
    @Override boolean isComplextContent() { false }

    /** a Body is a virtual modeling element that helps make mapping more explicit */
    Body getBody() { new Body(parent:this, type:base, mixedContent:mixedContent) }
    @Override boolean isBody() { !isEmpty() }
    @Override boolean isMixed() { isBody() && mixedContent }
}
