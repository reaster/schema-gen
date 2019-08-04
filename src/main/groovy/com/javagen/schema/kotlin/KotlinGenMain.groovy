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

package com.javagen.schema.kotlin

import com.javagen.schema.common.PluralService
import com.javagen.schema.common.PluralServiceNoop

import static com.javagen.schema.common.GlobalFunctionsUtil.javaEnumName

/**
 * This class is only used for testing and development.
 *
 * @author Richard Easterling
 */
class KotlinGenMain extends KotlinGen
{
    def initWadl()
    {
        schemaURL = new URL('file:../schema-gen-examples/wadl/src/main/resources/wadl.xsd')
        srcDir = new File('../schema-gen-examples/wadl/src/main/kotlin-gen')
        pluralService = new PluralServiceNoop()
        rootElements = ['application'] as Set
        packageName = 'org.w3.xml.namespace.kotlin' //avoid Java-Kotlin clash
    }

    def initGpx()
    {
        schemaURL = new File('../schema-gen-examples/kotlin-gpx/src/main/resources/gpx.xsd').toURI().toURL()
        srcDir = new File('../schema-gen-examples/kotlin-gpx/src/main/kotlin-gen')
    }

    def initHsf()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/hsf-1_1.xsd').toURI().toURL()
        srcDir = new File('../schema-gen-hsf/hsf-kotlin/src/main/kotlin-gen')
        println "srcDir = ${srcDir.getAbsolutePath()}"
        customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
        def enumCustomNames = ['primitive+':'PrimitivePlus','$':'Cheap','$$':'Moderate','$$$':'Pricy','$$$$':'Exclusive']
        def unknownEnum = 'Unknown'
        enumNameFunction = { text -> text.contains('?') ? unknownEnum : enumCustomNames[text] ?: javaEnumName(text, false) }
    }

    KotlinGenMain()
    {
        super()
        //initGpx()
        initHsf()
        //initWadl()
    }

    static void main(String[] args) {
        new KotlinGenMain().gen()
    }

}
