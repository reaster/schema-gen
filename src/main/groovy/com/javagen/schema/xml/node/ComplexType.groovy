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

import groovy.transform.ToString

/**
 * ComplexTypes can have attributes, elements and a body. ComplexTypes are generally mapped to a MClass with the
 * exception of wrapperClasses which can be mapped to a parametrized container (i.e. List<ChildElement>).
 */
@ToString(includeSuper=true,includePackage=false)
class ComplexType extends SimpleType implements CompositorHolder
{
    /** wrapper elements can be mapped directly to a container (i.e. List<ChildElement>) and don't need their own class */
    @Override boolean isWrapperElement(boolean allowAnyChild=false) {
//        if (qname.name.startsWith('ptseg') || qname.name.startsWith('extensions'))
//            println qname.name
        if (!attributes.isEmpty())
            return false
        def elements = childElements()
        if (elements.size() != 1)
            return false
        Element e = elements[0]
        boolean isAnyClass = (e instanceof Any)
        boolean isOptionalAnyChild = allowAnyChild ? true : !isAnyClass
        boolean result = e.maxOccurs > 1 && !e.isMixed() && isOptionalAnyChild
//        if (result)
//            println("isWrapperElement: ${this}")
        result
    }
    @Override TextOnlyType wrapperType() {
        if ( isWrapperElement() )
            childElements()[0].type
        else
            null
    }
    /** @return true if contains a single value, text only content. */
    @Override boolean isSimpleType() { false }
    /** @return true if contains text only content and attributes. */
    @Override boolean isSimpleContent() { false }
    /** @return true if contains child elements and attributes. */
    @Override boolean isComplextContent() { true }
//    @Override boolean isEmpty() {
//        base == null || !(childElements().size() == 1 && childElements()[0] instanceof Any)
//    }
    @Override boolean isBody() {
        boolean result = elementCount() == 1 && childElements()[0] instanceof Any && childElements()[0].maxOccurs == 1
        result
    }

    @Override Body getBody() {
        if (elementCount() == 1 && childElements()[0] instanceof Any && childElements()[0].maxOccurs == 1) {
            Any any = childElements()[0]
            new Body(parent: this, element: any, mixedContent: mixedContent)
        } else if (base != null) {
            super.getBody()
        } else {
            null
        }
    }


//    @Override String toString()
//    {
//        def s = qname ? "ComplexType: ${qname.name} [\n" : 'ComplexType[\n'
//        if (qname?.name == 'SubPremiseType')
//            println qname.n
//        if (attributes) {
//            s+= '  attributes[\n'
//            for(def a : attributes)
//                s+= "    ${a.qname.name}=${a.type ? a.type.qname.name : '?'}\n"
//            s+= '  ]\n'
//        }
//        if (elements) {
//            s+= '  elements[\n'
//            for(def e : elements)
//                s+= "    ${e}\n"
//            s+= '  ]\n'
//        }
//        s+= ']\n'
//        s
//    }

}
