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

package com.javagen.schema.java

import com.javagen.schema.common.PluralService
import com.javagen.schema.common.PluralServiceNoop

import static com.javagen.schema.common.GlobalFunctionsUtil.javaEnumName

/**
 * This class is the entry point for Java code generation.
 *
 * TODO migrate SchemaToJava into this class
 *
 * @author Richard Easterling
 */
class JavaGen extends SchemaToJava
{
	def initGpx()
	{
		schemaURL = new File('/Users/richard/dev/hs/xml-xml/example-gpx-java/src/main/resources/gpx.xsd').toURI().toURL()
		srcDir = new File('example-gpx-java/src/main/java-gen')
	}

	def initHsf()
	{
		schemaURL = new File('/Users/richard/dev/hs/hsf-data/hsf-1_1.xsd').toURI().toURL()
		packageName = 'com.hotspringsfinder.detail.model'
		srcDir = new File('example-hsf-java/src/main/java-gen')
		customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
		pluralService = new PluralService(customPluralMappings)
		def enumCustomNames = ['primitive+':'PrimitivePlus','$':'Cheap','$$':'Moderate','$$$':'Pricy','$$$$':'Exclusive']
		def unknownEnum = 'Unknown'
		enumNameFunction = { text -> text.contains('?') ? unknownEnum : enumCustomNames[text] ?: javaEnumName(text, false) }
	}

	JavaGen()
	{
		super(false)
//		schemaURL = new URL('file:../schema-gen-examples/wadl/src/main/resources/wadl.xsd')
//		srcDir = new File('../schema-gen-examples/wadl/src/main/java-gen')
//		pluralService = new PluralServiceNoop()
//		rootElements = ['application'] as Set

//		srcDir = new File('example-atom-java/src/main/java-xml')
//		schemaURL = new File('/Users/richard/dev/hs/xml-xml/example-atom-java/src/main/resources/atom.xsd').toURI().toURL()
//		packageName = 'org.w3.atom'
//		addSuffixToEnumClass = null
		//schemaURL = new File('/Users/richard/dev/hs/xml-xml/example-x-java/src/main/resources/xAL.xsd').toURI().toURL()
		//schemaURL = new URL('http://docs.oasis-open.org/election/external/xAL.xsd')
//		initGpx()
//		initHsf()
	}

	static void main(String[] args) {
		new JavaGen().gen()
	}

}
