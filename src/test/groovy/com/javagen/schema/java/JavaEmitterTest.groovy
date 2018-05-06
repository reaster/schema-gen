package com.javagen.schema.java

import com.javagen.schema.common.Gen
import com.javagen.schema.common.TestGen
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MField
import com.javagen.schema.model.MMethod
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.model.MReference
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test

import static com.javagen.schema.model.MCardinality.ARRAY
import static com.javagen.schema.model.MCardinality.LIST
import static com.javagen.schema.model.MCardinality.MAP
import static com.javagen.schema.model.MCardinality.OPTIONAL
import static com.javagen.schema.model.MCardinality.SET
import static com.javagen.schema.model.MMethod.IncludeProperties.allProperties
import static com.javagen.schema.model.MMethod.IncludeProperties.finalProperties
import static com.javagen.schema.model.MMethod.Stereotype.constructor
import static com.javagen.schema.model.MMethod.Stereotype.equals
import static com.javagen.schema.model.MMethod.Stereotype.hash
import static com.javagen.schema.model.MMethod.Stereotype.toString
import static junit.framework.TestCase.assertTrue

/**
 * Test Java code generation from models.
 */
class JavaEmitterTest
{
	Gen gen
	JavaEmitter visitor
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
		os = new ByteArrayOutputStream()
		visitor = new JavaEmitter(gen:gen, openStreamLambda: { File f -> new PrintStream(os) })

		module = new MModule(name: 'com.hotspringsfinder.model')
		phone = new MClass(name: 'Phone')
		phone.addField(new MProperty(name: 'number'))
		address = new MClass(name: 'Address')
		address.addField(new MField(name: 'city', 'static': true))
		address.addField(new MProperty(name: 'street1'))
		address.addField(new MProperty(name: 'zip',scope: 'public', type: 'int', val: 99999))
		address.addField(new MProperty(name: 'planet', scope: 'public', 'final': true, val: '"Earth"'))
		address.addField(new MReference(name: 'phones', cardinality: LIST, type: phone))
		address.addMethod(new MMethod(name: 'getCentury', 'static': true, type: 'int', body: { m, v, s -> v.out << '\n' << v.tabs << 'return 21;' }))
		module.addClass(address)
		module.addClass(phone)
	}

	@Test
	void testConstructor()
	{
		address.addMethod(new MMethod(stereotype: constructor))
		address.addMethod(new MMethod(stereotype: constructor, includeProperties: allProperties))
		address.addMethod(new MMethod(stereotype: constructor, includeProperties: finalProperties))

		new JavaPreEmitter(gen:gen).visit(module)
		visitor.visit(module)

		String output = os.toString("UTF8")
		println output

		assertTrue( output.contains('Address() {'))
		assertTrue( output.contains('Address(String street1, int zip, String planet, java.util.List<Phone> phones)'))
		assertTrue( output.contains('Address(String planet)'))
	}

	@Test
	void testBasicVisitor()
	{
		address.addField(new MProperty(name: 'set', cardinality: SET, type: phone))
		address.addField(new MProperty(name: 'array', cardinality: ARRAY, type: phone))
		address.addField(new MProperty(name: 'opt', cardinality: OPTIONAL, val: 'Optional.of("BIG")'))
		address.addField(new MProperty(name: 'map', cardinality: MAP, attr: ['keyType':'Integer']))
		address.addMethod(new MMethod(name: 'collectMoney', 'abstract': true, type: 'double') )
		address.addMethod(new MMethod(name: 'foo', type: 'boolean') )
		def pre = new JavaPreEmitter(gen:gen)
		//pre.defaultMethods = EnumSet.of(equals, hash, toString)
		pre.visit(module)
		visitor.visit(module)
		String output = os.toString("UTF8");
		println output

		assertTrue( output.contains('package com.hotspringsfinder.model;'))
		assertTrue( output.contains('public class Address'))
		assertTrue( output.contains('String city'))
		assertTrue( output.contains('public static int getCentury()'))
		assertTrue( output.contains('public String getStreet1()'))
		assertTrue( output.contains('public void setStreet1(String street1)'))
		assertTrue( output.contains('List<Phone> getPhones()'))
		assertTrue( output.contains('public abstract double collectMoney();'))
		assertTrue( output.contains('public class Phone'))
		assertTrue( output.contains('public String getNumber()'))
		assertTrue( output.contains('public void setNumber(String number)'))
		assertTrue( output.contains('public void setMap(java.util.Map<Integer,String> map)'))
	}

	@Test
	void testStandAloneEnum()
	{
		module = new MModule(name: 'com.hotspringsfinder.model')
		def phoneEnum = new MEnum(name: 'PhoneEnum', enumNames: ['CELL', 'HOME', 'WORK', 'FAX'])
		module.addClass(phoneEnum)

		visitor.visit(module)
		String output = os.toString("UTF8");
		println output

		assertTrue(output.contains('package com.hotspringsfinder.model;'))
		assertTrue(output.contains('public enum PhoneEnum'))
		assertTrue(output.contains('CELL,'))
		assertTrue(output.contains('FAX;'))
	}

	@Test
	void testEnumWithValue()
	{
		module = new MModule(name: 'com.hotspringsfinder.model')
		def phoneEnum = new MEnum(name: 'PhoneEnum', enumNames: ['CELL', 'HOME', 'WORK', 'FAX'], enumValues: ['cell','home','work','fax'])
		//setup a private value addField
		phoneEnum.addField( new MProperty(name: 'value', scope: 'private', 'final': true) )
		//add a private constructor
		phoneEnum.addMethod( new MMethod(name:phoneEnum.shortName(), stereotype:constructor, includeProperties:allProperties, scope:'private') )
		module.addClass(phoneEnum)

		new JavaPreEmitter(gen:gen).visit(module)
		visitor.visit(module)
		String output = os.toString("UTF8");
		println output

		assertTrue( output.contains('CELL("cell"),'))
		assertTrue( output.contains('private final String value;'))
	}

	@Test
	void testNestedEnum()
	{
		module = new MModule(name: 'com.hotspringsfinder.model')
		def phoneEnum = new MEnum(name: 'PhoneEnum', enumNames: ['CELL', 'HOME', 'WORK', 'FAX'])
		def phone = new MClass(name: 'Phone')
		module.addClass(phone)
		phone.addClass(phoneEnum)

		visitor.visit(module)
		String output = os.toString("UTF8");
		println output

		assertTrue(output.contains('public enum PhoneEnum'))
	}

	@Test
	void testEquals()
	{
		def module = new MModule(name: 'com.hotspringsfinder.model')
		def phone = new MClass(name: 'Phone')
		phone.addField(new MProperty(name: 'number'))
		def address = new MClass(name: 'Address')
		module.addClass(address)
		address.addField(new MField(name: 'country', 'static': true))
		address.addField(new MProperty(name: 'street1'))
		address.addField(new MProperty(name: 'city'))
		address.addField(new MProperty(name: 'zip', type: 'int'))
		address.addField(new MReference(name: 'phones', cardinality: LIST, type: phone))
		address.addMethod(new MMethod(stereotype: equals))

		new JavaPreEmitter(gen:gen).visit(module)
		visitor.visit(module)
		String output = os.toString("UTF8");
		println output

		assertTrue( output.contains('boolean equals(Object o)'))
		assertTrue( output.contains('if (this == o) return true;'))
		assertTrue( output.contains('if (o == null || getClass() != o.getClass()) return false;'))
		assertTrue( output.contains('if (street1 != null ? !street1.equals(other.street1) : other.street1 != null) return false;'))
		assertTrue( output.contains('if (zip != other.zip) return false;'))
	}

	@Test
	void testHash()
	{
		def module = new MModule(name: 'com.hotspringsfinder.model')
		def phone = new MClass(name: 'Phone')
		phone.addField(new MProperty(name: 'number'))
		def address = new MClass(name: 'Address')
		module.addClass(address)
		address.addField(new MField(name: 'country', 'static': true))
		address.addField(new MProperty(name: 'street1'))
		address.addField(new MProperty(name: 'city'))
		address.addField(new MProperty(name: 'zip', type:MType.lookupType('int')))
		address.addField(new MReference(name: 'phones', cardinality: LIST, type: phone))
		address.addMethod(new MMethod(stereotype: hash))

		new JavaPreEmitter(gen:gen).visit(module)
		visitor.visit(module)
		String output = os.toString("UTF8");
		println output

		assertTrue( output.contains('public int hashCode()'))
		assertTrue( output.contains('result = 31 * result + (street1 != null ? street1.hashCode() : 0);'))
		assertTrue( output.contains('result = 31 * result + zip;'))
	}

	@Test
	void testToString()
	{
		def module = new MModule(name: 'com.hotspringsfinder.model')
		def phone = new MClass(name: 'Phone')
		phone.addField(new MProperty(name: 'number'))
		def address = new MClass(name: 'Address')
		module.addClass(address)
		address.addField(new MField(name: 'country', 'static': true))
		address.addField(new MProperty(name: 'street1'))
		address.addField(new MProperty(name: 'city'))
		address.addField(new MProperty(name: 'zip', type: 'int'))
		address.addField(new MReference(name: 'phones', cardinality: LIST, type: phone))
		address.addMethod(new MMethod(stereotype: toString))

		new JavaPreEmitter(gen:gen).visit(module)
		visitor.visit(module)
		String output = os.toString("UTF8");
		println output

		assertTrue( output.contains('public String toString()'))
		assertTrue( output.contains('StringBuilder sb = new StringBuilder("Address[");'))
		assertTrue( output.contains('sb.append("street1=").append(street1);'))
		assertTrue( output.contains('sb.append(", city=").append(city);'))
	}
}
