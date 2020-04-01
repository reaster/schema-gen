/*
 * Copyright (c) 2017 Outsource Cafe, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.javagen.schema.xml.node
//@ToString(includeSuper=true,includePackage=false)
abstract class Type extends Node
{
    Type base
    boolean builtInType = false
    boolean _abstract = false
    boolean isAbstract() { _abstract }
    void setAbstract(boolean _abstract) { this._abstract = _abstract }
    /** @return true if contains a single value, text only content. */
    boolean isSimpleType() { true }
    /** @return true if contains text only content and attributes. */
    boolean isSimpleContent() { false }
    /** @return true if contains child elements and attributes. */
    boolean isComplextContent() { false }
    /** node contains data, no sub-elements or attributes */
    boolean isBody() { false }
    boolean isMixed() { false }
    /**
     * Signals an empty element body with no child elements or text content (i.e. empty tag or attributes only).
     */
    boolean isEmpty() { false }
    /**
     * Indicates when to implement inheritance in mapping class relationships. When false element body is
     * typicaly mapped to a property.
     * @return true if inheriting from anything but a simple type
     */
    boolean isInheritedBaseType() { !isEmpty() && !base.isSimpleType() }
    boolean isWrapperElement() { false }
    TextOnlyType wrapperType() { null }
//    void setBase(String base) { this.base = new QName(name:base) }
//    void setBase(QName base) { this.base = base }
    String toString() { "${getClass().simpleName}[name=${qname?.name} base=${base?.qname?.name}]" }
}
