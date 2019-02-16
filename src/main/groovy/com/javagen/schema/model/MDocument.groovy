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

package com.javagen.schema.model

class MDocument
{
    //static Calendar creationDate = Calendar.instance
    //static String copyright = "Copyright (c) ${creationDate.get(Calendar.YEAR)} Outsource Cafe, Inc."
    static String tool = '<a href="https://github.com/reaster/schema-gen">schema-gen</a> generated code (https://github.com/reaster/schema-gen)'

    /** refers to XML Schema instance or other source document. */
    static String source = null
    static String namespace = null
    static String author = 'Richard Easterling'

    def statements = []
    def attr = [:]

    /** called for source files (MModule or MClass) */
    MDocument provisionForSource() {
        Calendar creationDate = Calendar.instance
        String copyright = "Copyright (c) ${creationDate.get(Calendar.YEAR)} Outsource Cafe, Inc."
        attr['creationDate'] = "generated on ${creationDate.time}"
        attr['tool'] = tool
        if (namespace)
            attr['namespace'] = "namespace: ${namespace}"
        if (source)
            attr['source'] = "source document: ${source}"
        attr['author'] = author
        attr['copyright'] = copyright
        //println attr['copyright']
        return this
    }
    MDocument provisionForNestedClass() {
        attr['author'] = author
        return this
    }
    boolean isEmpty() {
        !statements && !attr
    }
}
