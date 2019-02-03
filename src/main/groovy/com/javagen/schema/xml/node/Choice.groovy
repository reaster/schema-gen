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
 * A compositor that allows one and only one of the elements contained in the selected group to be present within the containing element.
 *
 * Parents: group, choice, sequence, complexType, restriction (simpleContent), extension (simpleContent), restriction (complexContent), extension (complexContent)
 * Content: (annotation?, (element | group | choice | sequence | any)*)
 *
 * @author Richard Easterling
 */
class Choice extends Compositor
{
    Polymorphic polymorphic = null

    Polymorphic polymorphicType() {
        if (polymorphic == null) {
            for(Element e : childElements()) { //includes elements and groups
                if (e.maxOccurs != 1 || (e instanceof Any)) {
                    polymorphic = Polymorphic.NOT_POLYMORPHIC
                    return polymorphic
                }
            }
            polymorphic = this.maxOccurs > 1 ? Polymorphic.COLLECTION : Polymorphic.SINGLE_VALUE
        }
        polymorphic
    }

    enum Polymorphic {
        COLLECTION,
        SINGLE_VALUE,
        NOT_POLYMORPHIC;
        boolean isPolymorphic() { COLLECTION == this || SINGLE_VALUE == this }
        //final EnumSet<Polymorphic> POLY_TYPES = EnumSet.of(COLLECTION, SINGLE_VALUE)
    }
}
