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
import com.javagen.schema.dart.DartUtil

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
        schemaURL = new URL('file:../schema-gen-examples/java-x/src/main/resources/xhtml4-strict-subset.xsd')
        packageName = 'com.hotspringsfinder.xhtml.model'
        srcDir = new File('../schema-gen-examples/java-x/src/main/java-gen')
        //srcDir = new File('../schema-gen-hsf/hsf-java/src/main/java-gen2')
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

    def initJavaGpx()
    {
        schemaURL = new File('../schema-gen-examples/java-gpx/src/main/resources/gpx.xsd').toURI().toURL()
        srcDir = new File('../schema-gen-examples/java-gpx/src/main/java-gen')
        anyPropeertyNameWrapped = 'anyMap'
    }

    def initGpx()
    {
        schemaURL = new File('../schema-gen-examples/gpx/src/main/resources/gpx.xsd').toURI().toURL()
        srcDir = new File('../schema-gen-examples/gpx/src/main/java-gen')
    }

    def initHsf()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/hsf-1_1.xsd').toURI().toURL()
        //packageName = 'com.hotspringsfinder.detail.model'
        srcDir = new File('../javagen/schema-gen-hsf/hsf-java/src/main/java-gen')
        customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
        def enumCustomNames = ['primitive+':'PrimitivePlus','$':'Cheap','$$':'Moderate','$$$':'Pricy','$$$$':'Exclusive']
        def unknownEnum = 'Unknown'
        enumNameFunction = { text -> text.contains('?') ? unknownEnum : enumCustomNames[text] ?: javaEnumName(text, false) }
    }
    def initAttraction()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/attractions-1_0.xsd').toURI().toURL()
        srcDir = new File('/Users/richard/dev/hs/attraction-model-v1/src/main/java-gen')
        customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
        polyMorphicListName = 'results'
        anyPropeertyNameWrapped = 'results'
    }
    def initHsf2Extension()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/hsf-extension-2_0.xsd').toURI().toURL()
        packageName = 'com.hotspringsfinder.model.ext.v2'
        srcDir = new File('/Users/richard/dev/hs/hsf-ext-v2/src/main/java-gen')
        treatWrapperElementsAsCollections = false
    }
    def initHsf2()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/hsf-2_0.xsd').toURI().toURL()
        packageName = 'com.hotspringsfinder.model.v2'
        //srcDir = new File('../javagen/schema-gen-hsf/hsf-java/src/main/java-gen')
        srcDir = new File('/Users/richard/dev/hs/hsf-model-v2/src/main/java-gen')
        customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
        polyMorphicListName = 'results'
        //enumNameFunction = { text -> DartUtil.dartEnumName(text, false, true) } //match client case
    }
    def initKml()
    {
        schemaURL = new URL('file:../schema-gen-examples/java-x/src/main/resources/ogckml22.xsd')
        //schemaURL = new URL('file:../schema-gen-examples/java-x/src/main/resources/atom-author-link.xsd')
        srcDir = new File('../schema-gen-examples/java-x/src/main/java-gen')
        rootElements = ['kml']
        printSchema = true
    }

    def initJava14Gpx()
    {
        schemaURL = new File('../schema-gen-examples/java-gpx/src/main/resources/gpx.xsd').toURI().toURL()
        srcDir = new File('../schema-gen-examples/java14-gpx/src/main/java-gen')
        pluralService = new com.javagen.schema.common.PluralServiceNoop()//property names must match tags for now
        //callback = new Java14JacksonCallback(this) //sets POJOs to records
        pipeline = [
            new Java14Emitter(),
            new JavaPreEmitter(gen: this),
            new JavaEmitter(gen: this)
        ]
    }

    JavaGenMain()
    {
        super()
//        initAtom() //working 3/14/2020
        //initKml()
		initJavaGpx() //working 3/14/2020
//        initJava14Gpx() //working 4/1/2020
//		initHsf2()
//        initHsf2Extension()
        //initAttraction()
        //initFactionDigits()
//        initXHTML()
    }

    static void main(String[] args) {
        new JavaGenMain().gen()
    }

}
