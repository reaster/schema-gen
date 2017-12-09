package com.javagen.gen.java

import com.javagen.gen.model.MModule
import com.javagen.gen.schema.XmlSchemaNormalizer
import com.javagen.gen.schema.node.Schema
import com.javagen.gen.util.PluralService

import static com.javagen.gen.util.GlobalFunctionsUtil.javaEnumName
import static com.javagen.gen.util.GlobalFunctionsUtil.stripNamespace

/**
 * Configure
 */
class JavaGen extends SchemaToJava
{

	def initGpx()
	{
		schemaFile = new File('/Users/richard/dev/hs/schema-gen/example-gpx-java/src/main/resources/gpx.xsd').toURI().toURL()
		srcDir = new File('example-gpx-java/src/main/java-gen')
	}

	def initHsf()
	{
		schemaFile = new File('/Users/richard/dev/hs/hsf-data/hsf-1_1.xsd').toURI().toURL()
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
//		srcDir = new File('example-atom-java/src/main/java-gen')
//		schemaFile = new File('/Users/richard/dev/hs/schema-gen/example-atom-java/src/main/resources/atom.xsd').toURI().toURL()
//		packageName = 'org.w3.atom'
//		addSuffixToEnumClass = null
		//schemaFile = new File('/Users/richard/dev/hs/schema-gen/example-x-java/src/main/resources/xAL.xsd').toURI().toURL()
		//schemaFile = new URL('http://docs.oasis-open.org/election/external/xAL.xsd')
//		initGpx()
//		initHsf()
	}

	static void main(String[] args) {
		new JavaGen().gen()
	}

}
