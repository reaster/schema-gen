/*
 * Copyright (c) 2020 Outsource Cafe, Inc.
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

package com.javagen.schema.java

import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MMethod
import com.javagen.schema.model.MModule
import static com.javagen.schema.model.MMethod.Stereotype.canonicalConstructor

/**
 * Flag eligible classes as Java 14 records (JEP 359).
 *
 * Default values not supported.
 */
class Java14Emitter extends CodeEmitter
{
    // see
    // https://openjdk.java.net/jeps/359
    // https://github.com/FasterXML/jackson-future-ideas/issues/46
    //
    @Override
    def visit(MModule m)
    {
        m.classes.each {
            visit(it)
        }
        m.children.values().each { //visit submodules
            visit(it)
        }
    }

    @Override
    def visit(MClass c)
    {
        /** Java 14 records can't extend a class or be an interface */
        if (!c.interface && !c.extends) {
            c.data = true
            optionalCanonicalConstructor(c)
            MClass parentClass = c.parent instanceof MClass ? c.parent : null
            if (parentClass?.data)
                c.static = false // nested records implicitly static
        }
        c.classes.each {
            visit(it)
        }
    }

    /**
     * just enforces final constants for now, throwing IllegalArgumentException when not provided
     */
    def optionalCanonicalConstructor(MClass c)
    {
        def constFields = c.fields.values().findAll { !it.isStatic() && it.isFinal() && it.val }
        if (!constFields.isEmpty()) {
            c.addMethod(new MMethod(stereotype: canonicalConstructor))
        }
    }
}
