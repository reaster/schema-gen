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

package com.javagen.schema.common

import com.javagen.schema.model.MRestriction
import com.javagen.schema.xml.node.Attribute
import com.javagen.schema.xml.node.Element
import com.javagen.schema.xml.node.Restriction
import static com.javagen.schema.model.MRestriction.Type
import static com.javagen.schema.xml.node.Restriction.RType

class MappingUtil
{
    static MRestriction translate(Restriction restriction)
    {
        switch (restriction.type) { //enummeration, min, minExclusive, max, maxExclusive, length, regexp, notnull
            case RType.enumeration: return new MRestriction(Type.enummeration , restriction.value)
            case RType.minInclusive: return new MRestriction(Type.min, restriction.value)
            case RType.maxInclusive: return new MRestriction(Type.max, restriction.value)
            case RType.minExclusive: return new MRestriction(Type.minExclusive, restriction.value)
            case RType.maxExclusive: return new MRestriction(Type.maxExclusive, restriction.value)
            case RType.pattern: return new MRestriction(Type.regexp, restriction.value)
            case RType.whiteSpace: return null//new MRestriction(Type.enummeration , restriction.value)
            case RType.length: return new MRestriction(Type.length, restriction.value)
            case RType.minLength: return new MRestriction(Type.min, restriction.value)
            case RType.maxLength: return new MRestriction(Type.max, restriction.value)
            case RType.fractionDigits: return null//return new MRestriction(Type.enummeration , restriction.value)
            case RType.totalDigits: return null//return new MRestriction(Type.enummeration , restriction.value)
            default:
                throw new IllegalArgumentException("${restriction} not yet supported")
        }
    }

    static List<MRestriction> translate(List<Restriction> restrictions)
    {
        List<MRestriction> results = []
        if (restrictions) {
            for(Restriction restriction : restrictions) {
                MRestriction result = translate(restriction)
                if (result)
                    results << result
            }
        }
        results
    }

    static List<MRestriction> translate(Element element)
    {
        List<MRestriction> results = []
        if (element.type.restrictions) {
            for(Restriction restriction : element.type.restrictions) {
                MRestriction result = translate(restriction)
                if (result)
                    results << result
            }
        }
        results << new MRestriction(Type.min, element.minOccurs)
        if (element.maxOccurs < Integer.MAX_VALUE)
            results << new MRestriction(Type.max, element.maxOccurs)
        results
    }
    static List<MRestriction> translate(Attribute attribute)
    {
        List<MRestriction> results = []
        if (attribute.type.restrictions) {
            for(Restriction restriction : attribute.type.restrictions) {
                MRestriction result = translate(restriction)
                if (result)
                    results << result
            }
        }
        if (attribute.required)
            results << new MRestriction(Type.min, 1)
        results
    }
}
