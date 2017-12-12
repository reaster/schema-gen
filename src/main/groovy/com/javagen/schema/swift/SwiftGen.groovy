package com.javagen.schema.swift
/**
 * Configure
 */
class SwiftGen extends SchemaToSwift
{

	SwiftGen()
	{
		super()
		//schemaFile = new URL('http://www.topografix.com/gpx/1/1/gpx.xsd')
		//schemaFile = new File('/Users/richard/dev/hs/hsf-data/hsf-1_1.xsd')
//		schemaFile = new File('../schema-gen-examples/swift-gpx/src/resources/gpx.xsd').toURI().toURL()
//		srcDir = new File('../schema-gen-examples/swift-gpx/src/swift-gen')
		//this.validateable = false
		//this.singleFile = false
		//this.packageName = 'com.hotspringsfinder.detail.model'
		//this.xmlPackageName = 'com.hotspringsfinder.detail.xml'
		//this.srcDir = new File('/Users/richard/dev/hs/sandbox/hsf-model2/src/main/java')
		//this.xmlMapper = true
//		this.rootTags = ['gpx', 'hotSpringsMetadata']
//		this.nsPrefix = 'p'
//		this.nsURL = 'http://www.hotspringsfinder.com/schema/meatadata/1/1'
//		this.schemaLocation = this.nsURL+' ../'+schemaFile+' '
//		this.defaultTextBodyProperty = 'note'
//		this.skipTags = ["facilities","services","photos"]
//		this.customTextPropertyNameForClass = ['Leg':'directions','Email':'address','Phone':'number']
//		//this.customElementPropertyName = ['cmt':'desc']
//		//def timeFormatter = 'new java.text.SimpleDateFormat("HH:mm:ss")'
//		//this.formatters = ['hours/close':timeFormatter, 'hours/open':timeFormatter]
//		this.customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
//		//enum handling
//		def enumCustomNames = ['primitive+':'PrimitivePlus','$':'Cheap','$$':'Moderate','$$$':'Pricy','$$$$':'Exclusive']
//		def unknownEnum = 'Unknown'
//		this.enumNameFunction = { text -> text.contains('?') ? unknownEnum : enumCustomNames[text] ?: GlobalFunctionsUtil.javaEnumName(text, false) }
////		def customEnumValues = ['???':'?', '??': '?']
//		this.enumValueFunction = { text -> text.contains('?') ? '?' : text }
	}

	static void main(String[] args) { new SwiftGen().gen() }
}
