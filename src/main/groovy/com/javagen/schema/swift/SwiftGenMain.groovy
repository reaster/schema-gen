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

package com.javagen.schema.swift

import com.javagen.schema.common.PluralServiceNoop

import static com.javagen.schema.swift.SwiftUtil.swiftEnumName
import static com.javagen.schema.swift.SwiftUtil.legalSwiftPropertytName
import static com.javagen.schema.common.GlobalFunctionsUtil.javaEnumName

/**
 * This class is only used for testing and development.
 *
 * @author Richard Easterling
 */
class SwiftGenMain extends SwiftGen
{
    def initHsfProducts()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/products/hsf-products-1_2.xsd').toURI().toURL()
        packageName = 'com.hotspringsfinder.product.model'
        srcDir = new File('../schema-gen-hsf/hsf-swift/src/main/swift-gen')
        //defaultEnumValue = 'unknown'
        enumNameFunction = {
            text -> text.contains('?') ? 'unknown' : text.toLowerCase() }
    }
    def initHsf()
    {
        schemaURL = new File('/Users/richard/dev/hs/hsf-data/hsf-1_1.xsd').toURI().toURL()
        packageName = 'com.hotspringsfinder.detail.model'
        srcDir = new File('../schema-gen-hsf/hsf-swift/src/main/swift-gen')
        customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
        def enumCustomNames = ['primitive+':'primitivePlus','$':'cheap','$$':'moderate','$$$':'pricy','$$$$':'exclusive']
        //defaultEnumValue = 'unknown'
        enumNameFunction = {
            text -> text.contains('?') ? 'unknown' : enumCustomNames[text] ?: swiftEnumName(text, false) }
    }
    def initAtom()
    {
        srcDir = new File('../schema-gen-examples/java-atom/src/main/swift-gen')
        schemaURL = new File('../schema-gen-examples/java-atom/src/main/resources/atom.xsd').toURI().toURL()
        packageName = 'org.w3.atom.java'
        addSuffixToEnumClass = null
        anyPropertyName = 'text'
    }
    def initWadl()
    {
        schemaURL = new URL('file:../schema-gen-examples/wadl/src/main/resources/wadl.xsd')
        srcDir = new File('../schema-gen-examples/wadl/src/main/swift-gen')
        pluralService = new PluralServiceNoop()
        rootElements = ['application'] as Set
    }
    def initGpx()
    {
        schemaURL = new File('../schema-gen-examples/java-gpx/src/main/resources/gpx.xsd').toURI().toURL()
        srcDir = new File('../schema-gen-examples/java-gpx/src/main/swift-gen')
    }

    SwiftGenMain()
    {
        super()
        initHsfProducts()
    }

    static void main(String[] args) { new SwiftGenMain().gen() }

}
