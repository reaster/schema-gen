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

//@ToString(includeNames = true,includeSuper=true,includePackage=false,ignoreNulls = true)
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
    /** This element can substitute for the substitutionGroup element. Used to create polymorphic collections. */
    Element substitutionGroup
    /** @return true if element contains text and/or child elements */
    boolean isBody() { type?.isBody() ?: false }
    boolean isMixed() {
        type?.mixed ?: false
    }
    void setAbstract(boolean _abstract) { this._abstract = _abstract }
    boolean isAbstract() { _abstract }
    boolean getAbstract(boolean _abstract) { _abstract }
    @Override String toString() {
        def s = 'Element['
        if (qname) s += "$qname.name "
        if (minOccurs!=1 || maxOccurs!=1) s += "$minOccurs..$maxOccurs "
        if (type) s+= "$type.qname.name "
        if (isAbstract()) s += 'abstract '
        if (nillable) s += 'nillable '
        if (isAbstract()) s += 'abstract '
        if (substitutionGroup) s += "substitutionGroup:$substitutionGroup.qname.name "
        if (getDefault()) s += "= ${getDefault()} "
        if (fixed) s += "= ${fixed} "
        if (qname) s += "$qname.namespace"
        s += ']'
        s
    }
}
