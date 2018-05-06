package com.javagen.schema.swift

import com.javagen.schema.common.Gen
import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.common.TestGen
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MMethod
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.model.MReference
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry
import com.javagen.schema.model.MCardinality
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static com.javagen.schema.model.MMethod.IncludeProperties.allProperties
import static com.javagen.schema.model.MMethod.IncludeProperties.finalProperties
import static com.javagen.schema.model.MMethod.Stereotype.constructor
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
		visitor = new SwiftEmitter(gen: gen, openStreamLambda: { f -> new PrintStream(os) })

		module = new MModule(name: 'com.hotspringsfinder.model')
		module.sourceFile = new File('src/main/junk/Happy.swift') //trigger call to openStreamLambda
		phone = new MClass(name: 'Phone').setStruct(true)
		module.addClass(phone)
		MType.registerType(phone)
		phone.imports << 'Foundation'
		phone.addField(new MProperty(name: 'number', scope: 'public'))
		address = new MClass(name: 'Address').setStruct(true)
		module.addClass(address)
		MType.registerType(address)
		address.addField(new MProperty(name: 'city', 'static': true, scope: 'public'))
		address.addField(new MProperty(name: 'street1', scope: 'public', cardinality: MCardinality.OPTIONAL))
		address.addField(new MProperty(name: 'zip',scope: 'public', type: MType.lookupType('Int'), val: 99999))
		address.addField(new MProperty(name: 'planet', scope: 'public', 'final': true, val: 'Earth'))
		address.addField(new MReference(name: 'phones', cardinality: MCardinality.LIST, type: phone, scope: 'public'))
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
		address.addMethod(new MMethod(name: 'foo', body: { MMethod m, CodeEmitter visitor ->
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
		//SwiftPreEmitter.defaultMethods = EnumSet.of(hash,constructor) and defaults to allProperties if not specified
		//address.addMethod(new MMethod(stereotype: constructor))
		//address.addMethod(new MMethod(stereotype: constructor, includeProperties: allProperties))
		//address.addMethod(new MMethod(stereotype: constructor, includeProperties: finalProperties))

		new SwiftPreEmitter(gen:gen).visit(module)
		visitor.visit(module)

		String output = os.toString("UTF8");
		println output

		//assertTrue( output.contains('init() {'))
		assertTrue( output.contains('public init(number: String = "")'))
		assertTrue( output.contains('public init(street1: String? = nil, zip: Int = 99999, phones: [Phone] = [])'))
	}
/*



import Foundation

public struct Phone
{
    public var number: String = ""

    public init(number: String = "") {
        self.number = number
    }
}

public struct Address
{
    public static var city: String = ""
    public var street1: String?
    public var zip: Int = 99999
    public let planet: String = "Earth"
    public var phones: [Phone] = []

    public static func getCity() {
        //TODO
    }
    public func addressLabel() {
        //TODO
    }
    public init(street1: String? = nil, zip: Int = 99999, phones: [Phone] = []) {
        self.street1 = street1
        self.zip = zip
        self.phones = phones
    }
}

extension Phone: Hashable
{
    public var hashValue: Int {
        var result = 1
        result = 31 * result + number.hashValue
        return result;
    }

    public static func ==(rhs: Phone, lhs: Phone) -> Bool {
        guard lhs.number == rhs.number else { return false }
        return true;
    }
}

extension Address: Hashable
{
    public var hashValue: Int {
        var result = 1
        if let street1 = street1 { result = 31 * result + street1.hashValue }
        result = 31 * result + zip.hashValue
        result = 31 * result + planet.hashValue
        result = 31 * result + phones.count
        return result;
    }

    public static func ==(rhs: Address, lhs: Address) -> Bool {
        guard lhs.street1 == rhs.street1 else { return false }
        guard lhs.zip == rhs.zip else { return false }
        guard lhs.planet == rhs.planet else { return false }
        guard lhs.phones == rhs.phones else { return false }
        return true;
    }
}
 */
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
		module.sourceFile = new File('src/main/junk/Happy.swift') //trigger call to openStreamLambda
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
		module.sourceFile = new File('src/main/junk/Happy.swift') //trigger call to openStreamLambda
		def phoneEnum = new MEnum(name: 'PhoneEnum', enumNames: ['CELL', 'HOME', 'WORK', 'FAX'], enumValues: ['cell','home','work','fax'], 'implements': ['String','Codable'])
		module.addClass(phoneEnum)
		visitor.visit(module)

		String output = os.toString("UTF8");
		println output

		assertTrue(output!=null)
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
