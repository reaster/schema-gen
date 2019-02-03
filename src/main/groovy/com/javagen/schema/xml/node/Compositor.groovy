/*
 * Copyright (c) 2018 Outsource Cafe, Inc.
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
 * Compositors determine what, how many and in what order elements can be nested and sequenced together. The valid
 * subtypes are All, Choice and Sequence.
 *
 * Implementing classes: ComplexType, Group, All, Choice, Sequence
 * Content: (element | group | choice | sequence | any)*)
 *
 * @author Richard Easterling
 */
abstract class Compositor extends Node implements CompositorHolder
{
    /** min allowed occurrences. Defaults to 1. */
    int minOccurs = 1
    /** max allowed occurrences. Defaults to 1. unbounded is converted to Integer.MAX */
    int maxOccurs = 1
    int uboundedChildElements = 0
    boolean isUboundedChildElement() { uboundedChildElements > 0 }


    /** rather than actually trying to compute this, we just put it in the id attribute in the form: '*polymorphic-{rootTypeName}' */
    String polymporphicRootTypeName()
    {
        String typeName = 'anyType'
        if (id?.contains('polymorphic-')) {
            int index = id.indexOf('polymorphic-') + 'polymorphic-'.length()
            typeName = id.substring(index)
        }
        return typeName
    }
}
