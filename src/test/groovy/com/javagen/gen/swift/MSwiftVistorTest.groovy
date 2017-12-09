package com.javagen.gen.swift

import com.javagen.gen.Gen
import com.javagen.gen.MVisitor
import com.javagen.gen.TestGen
import com.javagen.gen.gen.model.*
import com.javagen.gen.model.MClass
import com.javagen.gen.model.MEnum
import com.javagen.gen.model.MMethod
import com.javagen.gen.model.MModule
import com.javagen.gen.model.MProperty
import com.javagen.gen.model.MReference
import com.javagen.gen.model.MType
import com.javagen.gen.model.MTypeRegistry
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static com.javagen.gen.model.MMethod.IncludeProperties.allProperties
import static com.javagen.gen.model.MMethod.IncludeProperties.finalProperties
import static com.javagen.gen.model.MMethod.Stereotype.constructor
import static junit.framework.TestCase.assertTrue

class MSwiftVistorTest
{
	Gen gen
	SwiftEmitter visitor
	ByteArrayOutputStream os

	MModule module = null
	MClass phone = null
	MClass address = null

	@BeforeClass
	static void init()
	{
		MTypeRegistry.reset()
	}

	@Before
	void setup()
	{
		gen = new TestGen()
		os = new ByteArrayOutputStream();
		visitor = new SwiftEmitter(gen: gen, out: new PrintStream(os))

		module = new MModule(name: 'com.hotspringsfinder.model')
		phone = new MClass(name: 'Phone').setStruct(true)
		module.addClass(phone)
		MType.registerType(phone)
		phone.imports << 'Foundation'
		phone.addField(new MProperty(name: 'number', scope: 'public'))
		address = new MClass(name: 'Address').setStruct(true)
		module.addClass(address)
		MType.registerType(address)
		address.addField(new MProperty(name: 'city', 'static': true, scope: 'public'))
		address.addField(new MProperty(name: 'street1', scope: 'public', cardinality: Container.OPTIONAL))
		address.addField(new MProperty(name: 'zip',scope: 'public', type: MType.lookupType('Int'), val: 99999))
		address.addField(new MProperty(name: 'planet', scope: 'public', 'final': true, val: 'Earth'))
		address.addField(new MReference(name: 'phones', cardinality: Container.LIST, type: phone, scope: 'public'))
		address.addMethod(new MMethod(name: 'getCity', 'static': true))
		address.addMethod(new MMethod(name: 'addressLabel'))
	}

	@Test
	void testBasicVisitor()
	{
		visitor.visit(module)

		String output = os.toString("UTF8");
		println output

		assertTrue( output.contains('import Foundation'))
		assertTrue( output.contains('struct Address'))
		assertTrue( output.contains('var street1: String'))
		assertTrue( output.contains('static var city: String'))
		assertTrue( output.contains('let planet: String'))
//		assertTrue( output.contains('public void setStreet1(String street1)'))
		assertTrue( output.contains('var phones: [Phone] = []'))

		assertTrue( output.contains('struct Phone'))
		assertTrue( output.contains('var number: String'))
	}

	@Test
	void testMethodBody()
	{
		address.addMethod(new MMethod(name: 'foo', body: { MMethod m, MVisitor visitor ->
			visitor.out << '\n' << visitor.tabs << 'print "I\'m lambda foo"'
		}))

		visitor.visit(module)
		String output = os.toString("UTF8");
		println output

		assertTrue( output.contains('func foo()') )
		assertTrue( output.contains('print "I\'m lambda foo"') )
	}


	@Test
	void testConstructor()
	{
		address.addMethod(new MMethod(stereotype: constructor))
		address.addMethod(new MMethod(stereotype: constructor, includeProperties: allProperties))
		address.addMethod(new MMethod(stereotype: constructor, includeProperties: finalProperties))

		new SwiftPreEmitter(gen:gen).visit(module)
		visitor.visit(module)

		String output = os.toString("UTF8");
		println output

		assertTrue( output.contains('init() {'))
		assertTrue( output.contains('init(street1: String?, zip: Int, planet: String, phones: [Phone]) {'))
		assertTrue( output.contains('init(planet: String)'))
	}

	@Test
	void testEquatable()
	{
		address.addMethod(new MMethod(stereotype: MMethod.Stereotype.equals))

		new SwiftPreEmitter(gen:gen).visit(module)
		visitor.visit(module)
		String output = os.toString("UTF8");
		println output

		assertTrue( output.contains('extension Address: Equatable'))
		assertTrue( output.contains('static func ==(rhs: Address, lhs: Address) -> Bool'))
		assertTrue( output.contains('guard lhs.street1 == rhs.street1 else { return false }'))
	}

	@Test
	void testHashable()
	{
		address.addMethod(new MMethod(stereotype: MMethod.Stereotype.hash))

		new SwiftPreEmitter(gen:gen).visit(module)
		visitor.visit(module)
		String output = os.toString("UTF8");
		println output
		assertTrue( output.contains('extension Address: Hashable'))
		assertTrue( output.contains('static func ==(rhs: Address, lhs: Address) -> Bool'))
		assertTrue( output.contains('guard lhs.street1 == rhs.street1 else { return false }'))

		assertTrue( output.contains('var hashValue: Int {'))
		assertTrue( output.contains('result = 31 * result + street1.hashValue'))
	}

	@Test
	void testStandAloneEnum()
	{
		module = new MModule(name: 'com.hotspringsfinder.model')
		def phoneEnum = new MEnum(name: 'PhoneEnum', enumNames: ['CELL', 'HOME', 'WORK', 'FAX'], 'implements': ['String', 'Codable'])
		module.addClass(phoneEnum)
		visitor.visit(module)

		String output = os.toString("UTF8");
		println output

		assertTrue(output.contains('enum PhoneEnum: String'))
		assertTrue(output.contains('case CELL'))
		assertTrue(output.contains('case FAX'))
	}

	@Test
	void testEnumWithValue()
	{
		module = new MModule(name: 'com.hotspringsfinder.model')
		def phoneEnum = new MEnum(name: 'PhoneEnum', enumNames: ['CELL', 'HOME', 'WORK', 'FAX'], enumValues: ['cell','home','work','fax'], 'implements': ['String','Codable'])
		module.addClass(phoneEnum)
		visitor.visit(module)

		String output = os.toString("UTF8");
		println output

		assertTrue(output.contains('enum PhoneEnum: String, Codable'))
		assertTrue(output.contains('case CELL = \"cell\"'))
	}

	@Test
	void testNestedEnum()
	{
		def phoneEnum = new MEnum(name: 'PhoneEnum', enumNames: ['CELL', 'HOME', 'WORK', 'FAX'], enumValues: ['cell','home','work','fax'], 'implements': ['String','Codable'], val: '.work')
		phone.addClass(phoneEnum)
		phone.addField( new MProperty(name: 'type', scope: 'public', type: phoneEnum) )
		visitor.visit(module)

		String output = os.toString("UTF8");
		println output

		assertTrue(output.contains('enum PhoneEnum: String, Codable'))
		assertTrue(output.contains('case CELL = \"cell\"'))
	}

}
