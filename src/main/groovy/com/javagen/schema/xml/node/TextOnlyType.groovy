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
 * A TextOnlyType describes an atomic type of an attribute or single value element and is typically employed to apply
 * extra restrictions to a basic xml type. In the simplest case, TextOnlyTypes are mapped directly to MProperty, but
 * depending on the restrictions complete mapping may require data validation rules (for example Java @Min/@Max/@RegExpr
 * annotations), enumeration classes, cardinality generation (when defined in a List element or defined as a multi-value
 * basic type like
 *
 * types
 */
//@ToString(includeSuper=true,includePackage=false)
class TextOnlyType extends Type
{
    java.util.List<Restriction> restrictions = []
    EnumSet<Restriction.RType> restrictionSet() {
        EnumSet<Restriction.RType> set = EnumSet.noneOf(Restriction.RType);
        restrictions.each{ set.add(it.type) };
        set
    }
    //boolean isEmpty() { false }
}
