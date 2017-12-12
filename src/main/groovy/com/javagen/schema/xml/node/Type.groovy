package com.javagen.schema.xml.node
//@ToString(includeSuper=true,includePackage=false)
abstract class Type extends Node
{
    Type base
    boolean builtInType = false
    /** @return true if contains a single value, text only content. */
    boolean isSimpleType() { true }
    /** @return true if contains text only content and attributes. */
    boolean isSimpleContent() { false }
    /** @return true if contains child elements and attributes. */
    boolean isComplextContent() { false }
    boolean isBody() { false }
    boolean isMixed() { false }
    boolean isWrapperElement() { false }
    TextOnlyType wrapperType() { null }
//    void setBase(String base) { this.base = new QName(name:base) }
//    void setBase(QName base) { this.base = base }
    String toString() { "${getClass().simpleName}[name=${qname?.name} base=${base?.qname?.name}]" }
}
