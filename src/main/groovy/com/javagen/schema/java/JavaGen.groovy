package com.javagen.schema.java

import com.javagen.schema.common.PluralService

import static com.javagen.schema.common.GlobalFunctionsUtil.javaEnumName

/**
 * Configure
 */
class JavaGen extends SchemaToJava
{

	def initGpx()
	{
		schemaFile = new File('/Users/richard/dev/hs/xml-xml/example-gpx-java/src/main/resources/gpx.xsd').toURI().toURL()
		srcDir = new File('example-gpx-java/src/main/java-xml')
	}

	def initHsf()
	{
		schemaFile = new File('/Users/richard/dev/hs/hsf-data/hsf-1_1.xsd').toURI().toURL()
		packageName = 'com.hotspringsfinder.detail.model'
		srcDir = new File('example-hsf-java/src/main/java-xml')
		customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
		pluralService = new PluralService(customPluralMappings)
		def enumCustomNames = ['primitive+':'PrimitivePlus','$':'Cheap','$$':'Moderate','$$$':'Pricy','$$$$':'Exclusive']
		def unknownEnum = 'Unknown'
		enumNameFunction = { text -> text.contains('?') ? unknownEnum : enumCustomNames[text] ?: javaEnumName(text, false) }
	}

	JavaGen()
	{
		super(false)
//		srcDir = new File('example-atom-java/src/main/java-xml')
//		schemaFile = new File('/Users/richard/dev/hs/xml-xml/example-atom-java/src/main/resources/atom.xsd').toURI().toURL()
//		packageName = 'org.w3.atom'
//		addSuffixToEnumClass = null
		//schemaFile = new File('/Users/richard/dev/hs/xml-xml/example-x-java/src/main/resources/xAL.xsd').toURI().toURL()
		//schemaFile = new URL('http://docs.oasis-open.org/election/external/xAL.xsd')
//		initGpx()
//		initHsf()
	}

	static void main(String[] args) {
		new JavaGen().gen()
	}

}
