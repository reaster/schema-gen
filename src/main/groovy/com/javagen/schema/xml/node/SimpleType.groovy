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
