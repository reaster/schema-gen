/*
 * Copyright (c) 2019 Outsource Cafe, Inc.
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

class Appinfo extends Node
{
    /**
     * Treat all appinfo elements that start with 'javagen:' as code generator program directives.
     */
    static final String JAVAGEN_PREFIX = 'javagen:'

    /**
     * Applied to group elements. Value should be name of group. Allows one type to 'extend' another, assuming groups are labeled as interfaces.
     */
    static final String EXTENDS_DIRECTIVE = JAVAGEN_PREFIX+'extends'

    /**
     * Applied to complexType. Value should be name of group or complexType. Allows a concrete type to 'implement' an interface, assuming types are labeled as interfaces.
     */
    static final String IMPLEMENTS_DIRECTIVE = JAVAGEN_PREFIX+'implements'

    /**
     * Applied to group elements. Value can be 'interface' or 'mixin'. An 'interface' labeled group will be treated as an interface,
     * verses being merged into the referencing complexType.
     * If value is 'mixin', group will be treated as an interface. Other classes that 'implement' the mixin
     * will have the mixin applied. Mixins are only supported in Dart.
     */
    static final String ABSTACTION_DIRECTIVE = JAVAGEN_PREFIX+'abstraction'

    /**
     * Applied to compexTypes. If the value is true, and type will be ignored (i.e. not emitted).
     */
    static final String IGNORE_DIRECTIVE = JAVAGEN_PREFIX+'ignore'

    /**
     * Applied to compexTypes. If the value is true, type will be treated as a mixin.
     * Other classes that 'implement' this class will have the mixin applied.
     */
    static final String TYPE_DIRECTIVE = JAVAGEN_PREFIX+'type'

    String text;

    /** given a key-value pair separated by an equals sign (javagen:type=String), return just the value. */
    String appinfoValue(String key) {
        text?.startsWith(key) ? text.replace("${key}=", '') : null
    }

}
