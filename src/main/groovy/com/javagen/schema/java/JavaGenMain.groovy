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

package com.javagen.schema.java

import com.javagen.schema.common.PluralService
import com.javagen.schema.common.PluralServiceNoop

import static com.javagen.schema.common.GlobalFunctionsUtil.javaEnumName

/**
 * This class is only used for testing and development.
 *
 * @author Richard Easterling
 */
class JavaGenMain extends JavaGen
{
    def initXHTML()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/xhtml1-strict-subset.xsd').toURI().toURL()
        packageName = 'com.hotspringsfinder.xhtml.model'
        srcDir = new File('../schema-gen-hsf/hsf-java/src/main/java-gen2')
    }
    def initAtom()
    {
        srcDir = new File('../schema-gen-examples/java-atom/src/main/java-gen')
        schemaURL = new File('../schema-gen-examples/java-atom/src/main/resources/atom.xsd').toURI().toURL()
        packageName = 'org.w3.atom.java'
        addSuffixToEnumClass = null
        anyPropertyName = 'text'
    }

    def initWadl()
    {
        schemaURL = new URL('file:../schema-gen-examples/wadl/src/main/resources/wadl.xsd')
        srcDir = new File('../schema-gen-examples/wadl/src/main/java-gen')
        pluralService = new PluralServiceNoop()
        rootElements = ['application'] as Set
    }
    def initAB()
    {
        schemaURL = new URL('file:../schema-gen-examples/java-x/src/main/resources/ab.xsd')
        srcDir = new File('../schema-gen-examples/java-x/src/main/java-gen')
    }
    def initFactionDigits()
    {
        schemaURL = new URL('file:./src/test/resources/fractions-digits.xsd')
        srcDir = new File('../schema-gen-examples/java-x/src/main/java-gen')
    }

    def initGpx()
    {
        schemaURL = new File('../schema-gen-examples/java-gpx/src/main/resources/gpx.xsd').toURI().toURL()
        srcDir = new File('../schema-gen-examples/java-gpx/src/main/java-gen')
    }

    def initHsf()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/hsf-1_1.xsd').toURI().toURL()
        packageName = 'com.hotspringsfinder.detail.model'
        srcDir = new File('../schema-gen-hsf/hsf-java/src/main/java-gen')
        customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
        def enumCustomNames = ['primitive+':'PrimitivePlus','$':'Cheap','$$':'Moderate','$$$':'Pricy','$$$$':'Exclusive']
        def unknownEnum = 'Unknown'
        enumNameFunction = { text -> text.contains('?') ? unknownEnum : enumCustomNames[text] ?: javaEnumName(text, false) }
    }
    def initHsf2()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/hsf-2_0.xsd').toURI().toURL()
        packageName = 'com.hotspringsfinder.model.v2'
        srcDir = new File('../schema-gen-hsf/hsf-java/src/main/java-gen')
        customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
        def enumCustomNames = ['primitive+':'PrimitivePlus','$':'Cheap','$$':'Moderate','$$$':'Pricy','$$$$':'Exclusive']
        def unknownEnum = 'Unknown'
        enumNameFunction = { text -> text.contains('?') ? unknownEnum : enumCustomNames[text] ?: javaEnumName(text, false) }
    }
    def initKml()
    {
        schemaURL = new URL('file:../schema-gen-examples/java-x/src/main/resources/ogckml22.xsd')
        //schemaURL = new URL('file:../schema-gen-examples/java-x/src/main/resources/atom-author-link.xsd')
        srcDir = new File('../schema-gen-examples/java-x/src/main/java-gen')
        rootElements = ['kml']
        printSchema = true
    }

    JavaGenMain()
    {
        super()
        //initAtom()
        //initKml()
        //schemaURL = new File('/Users/richard/dev/hs/xml-xml/example-x-java/src/main/resources/xAL.xsd').toURI().toURL()
        //schemaURL = new URL('http://docs.oasis-open.org/election/external/xAL.xsd')
		//initGpx()
		//initHsf2()
        initFactionDigits()
        //initXHTML()
    }

    static void main(String[] args) {
        new JavaGenMain().gen()
    }

}
