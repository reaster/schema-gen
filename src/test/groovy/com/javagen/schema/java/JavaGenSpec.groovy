package com.javagen.schema.java

import com.javagen.schema.model.MClass
import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.xml.XmlSchemaNormalizer

import static com.javagen.schema.xml.node.Schema.*
import com.javagen.schema.xml.node.*
import spock.lang.Shared
import spock.lang.Specification

class JavaGenSpec extends Specification
{
    @Shared def prefixToNamespaceMap = ['xsd':DEFAULT_NS, 'gpx':'http://www.topografix.com/GPX/1/1', targetNamespace:'http://www.topografix.com/GPX/1/1']

    def "module gen from schema"()
    {
        given:
        JavaGen schemaToJava = new JavaGen()
        when:
        Schema schema = new Schema(prefixToNamespaceMap:[targetNamespace:'http://www.topografix.com/GPX/1/1'])
        then:
        schema.prefixToNamespaceMap[targetNamespace] == 'http://www.topografix.com/GPX/1/1'
        when:
        schemaToJava.visit(schema)
        MModule module = schemaToJava.getModel()
        then:
        module != null
        module.name == 'com.topografix.gpx'
    }

    def "class gen from global SimpleType"()
    {
        given:
        JavaGen schemaToJava = new JavaGen()
        Schema schema = new Schema(prefixToNamespaceMap:prefixToNamespaceMap)
        when:
        SimpleType simpleType = new SimpleType(qname:schema.qname('Foo'),base:schema.getGlobal('xsd:string'))
        Attribute attr1 = new Attribute(qname:'age',type:schema.getGlobal('xsd:integer') )
        simpleType.attributes << attr1
        schema.putGlobal(simpleType)
        then:
        simpleType.isRoot() == true
        simpleType.qname.name == 'Foo'
        simpleType.qname.namespace == 'http://www.topografix.com/GPX/1/1'
        simpleType.base.qname.name == 'string'
        attr1.qname.name == 'age'
        attr1.type.qname.name == 'integer'
        simpleType.attributes[0] == attr1
        schema.getGlobal('gpx:Foo') != null
        when:
        schemaToJava.visit(schema)
        MModule module = schemaToJava.getModel()
        MClass foo = module.lookupClass('Foo')
        then:
        println foo
        foo != null
        foo.name == 'Foo'
        foo.fullName() == 'com.topografix.gpx.Foo'
    }

    def "test polymorphic any declaration"() {
        given:
        def xml = """<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://joe.org/schemmata" targetNamespace="http://joe.org/schemmata" elementFormDefault="qualified">
            <xsd:element name="reference" type="referenceType"/>
            <xsd:complexType name="referenceType">
                <xsd:attribute name="href" type="xsd:string"/>
                <xsd:attribute name="id" type="xsd:nonNegativeInteger"/>
                <xsd:attribute name="published" type="xsd:boolean"/>
            </xsd:complexType>
            <xsd:element name="list">
                <xsd:annotation><xsd:documentation>URL list of references to media (pictures, video, audio, ducuments).</xsd:documentation></xsd:annotation>
                <xsd:complexType>
                    <xsd:sequence>
                        <xsd:annotation><xsd:documentation>Polymorphic collection with referenceType as a base type.</xsd:documentation></xsd:annotation>
                        <xsd:any id="polymorphic-referenceType" minOccurs="0" maxOccurs="unbounded" processContents="lax"/>
                    </xsd:sequence>
                </xsd:complexType>
            </xsd:element>
                    </xsd:schema>"""
        JavaGen schemaVisitor = new JavaGen()
        when: "stage 1 - generate xml"
        Schema schema = new XmlSchemaNormalizer().buildSchema(xml)
        schemaVisitor.visit(schema)
        MModule module = schemaVisitor.model
        then: "root module generated containing classes"
        module != null
        module.classes.size() > 0
        module.classes.each { println it }
//        when: "ComplexType"
//        MClass joeClass = module.lookupClass('Joe')
//        then: "map to Java Class with properties"
//        joeClass != null
//        joeClass.fields.size() > 0
//        joeClass.fields.values().each { println it }
//        when: "maxOccurs=unbounded choice mapping"
//        MProperty names = joeClass.fields['names']
//        MProperty places = joeClass.fields['places']
//        then: "map to optional property"
//        names != null
//        names.cardinality == MCardinality.LIST
//        places != null
//        places.cardinality == MCardinality.LIST
    }

    def "test compositor variations"() {
        given:
        def xml = """<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://joe.org/schemmata" targetNamespace="http://joe.org/schemmata" elementFormDefault="qualified">
                        <xsd:element name="joe" type="joeType" />
                        <xsd:complexType name="joeType">
                            <xsd:choice minOccurs="0" maxOccurs="unbounded">
                                <xsd:element name="name" type="xsd:string"/>
                                <xsd:element name="place" type="xsd:string" />
                            </xsd:choice>
                        </xsd:complexType>
                    </xsd:schema>"""
        JavaGen schemaVisitor = new JavaGen()
        when: "stage 1 - generate xml"
        Schema schema = new XmlSchemaNormalizer().buildSchema(xml)
        schemaVisitor.visit(schema)
        MModule module = schemaVisitor.model
        then: "root module generated containing classes"
        module != null
        module.classes.size() > 0
        module.classes.each { println it }
        when: "ComplexType"
        MClass joeClass = module.lookupClass('Joe')
        then: "map to Java Class with properties"
        joeClass != null
        joeClass.fields.size() > 0
        joeClass.fields.values().each { println it }
        when: "maxOccurs=unbounded choice mapping"
        MProperty names = joeClass.fields['names']
        MProperty places = joeClass.fields['places']
        then: "map to optional property"
        names != null
        names.cardinality == MCardinality.LIST
        places != null
        places.cardinality == MCardinality.LIST
    }

    def "test element and attribute mapping variations"()
    {
        given:
        def xml = """<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns="http://www.topografix.com/GPX/1/1" targetNamespace="http://www.topografix.com/GPX/1/1" elementFormDefault="qualified">
                        <xsd:element name="wpt" type="wptType" />
                        <xsd:complexType name="wptType">
                            <xsd:sequence>
                                <xsd:element name="author" type="xsd:string" fixed="the dude"/>
                                <xsd:element name="ele" type="xsd:decimal" minOccurs="0" default="1.0"/>
                                <xsd:element name="time" type="xsd:dateTime" minOccurs="0" />
                                <xsd:element name="name" type="xsd:string" minOccurs="1" />
                                <xsd:element name="place" type="xsd:string" maxOccurs="unbounded" />
                                <xsd:element name="measure" type="measureType" />
                            </xsd:sequence>
                            <xsd:attribute name="fix" type="fixType" />
                            <xsd:attribute name="states" type="xsd:NMTOKENS" />
                            <xsd:attribute name="regions" type="regionListAttributeType" />
                            <xsd:attribute name="version" type="xsd:string" fixed="1.1" />
                            <xsd:attribute name="lat" type="latitudeType" use="required" />
                            <xsd:attribute name="lon" type="longitudeType" use="optional" />
                        </xsd:complexType>
                        <xsd:simpleType name="latitudeType">
                            <xsd:restriction base="xsd:double">
                                <xsd:minInclusive value="-90.0"/>
                                <xsd:maxInclusive value="90.0"/>
                            </xsd:restriction>
                        </xsd:simpleType>
                        <xsd:simpleType name="longitudeType">
                            <xsd:restriction base="xsd:double">
                                <xsd:minInclusive value="-180.0"/>
                                <xsd:maxExclusive value="180.0"/>
                            </xsd:restriction>
                        </xsd:simpleType>
                        <xsd:simpleType name="fixType">
                            <xsd:restriction base="xsd:string">
                                <xsd:enumeration value="none"/>
                                <xsd:enumeration value="2d"/>
                                <xsd:enumeration value="3d"/>
                            </xsd:restriction>
                        </xsd:simpleType>
                        <xsd:simpleType name="unitType">
                            <xsd:restriction base="xsd:string">
                                <xsd:enumeration value="none"/>
                                <xsd:enumeration value="dgps"/>
                                <xsd:enumeration value="pps"/>
                            </xsd:restriction>
                        </xsd:simpleType>
                        <xsd:simpleType name="regionListAttributeType">
                            <xsd:list itemType="xsd:string"/>
                        </xsd:simpleType>
                        <xsd:simpleType name="measureType">
                            <xsd:union memberTypes="fixType unitType"/>
                        </xsd:simpleType>
                    </xsd:schema>"""
        JavaGen schemaVisitor = new JavaGen()
        schemaVisitor.useOptional = true
        when: "stage 1 - generate xml"
        Schema schema = new XmlSchemaNormalizer().buildSchema(xml)
        then: "normalized xml produced"
        schema != null
        when: "xml ComplexType"
        ComplexType wptType = schema.getGlobal(schema.qname('wptType'))
        then: "normalized ComplexType produced"
        wptType != null
        when: "get composter from wpt"
        Sequence sequence = wptType.compositors.isEmpty() ? null : wptType.compositors.first()
        then: "should have sequence with 6 elements"
        sequence != null
        sequence.elements.size() == 6
        when: "stage 2 - generate object model from xml"
        schemaVisitor.visit(schema)
        MModule module = schemaVisitor.model
        then: "root module generated containing classes"
        module != null
        module.classes.size() > 3
        module.classes.each { println it }
        when: "ComplexType"
        MClass wptClass = module.lookupClass('Wpt')
        then: "map to Java Class with properties"
        wptClass != null
        wptClass.fields.size() >= 5
        wptClass.fields.values().each { println it }
        when: "NMTOKENS attribute"
        MProperty states = wptClass.fields['states']
        then: "map to LIST"
        states != null
        states.cardinality == MCardinality.LIST
        states.type.name == 'String'
        when: "simpleType list"
        MProperty regions = wptClass.fields['regions']
        then: "map to LIST"
        regions != null
        regions.cardinality == MCardinality.LIST
        regions.type.name == 'String'
        when: "fixed attribute"
        MProperty version = wptClass.fields['version']
        then: "map to required, final property"
        version != null
        version.cardinality == MCardinality.REQUIRED
        version.type.name == 'String'
        version.isFinal()
        version.val == '1.1'
        when: "required simpleType attribute"
        MProperty lat = wptClass.fields['lat']
        then: "map to required property"
        lat != null
        lat.cardinality == MCardinality.REQUIRED
        lat.type.name == 'double'
        when: "optional simpleType attribute"
        MProperty lon = wptClass.fields['lon']
        then: "map to optional property"
        lon != null
        when: "using Optional setting, primatives are put in primitva wrapper classes and placed in java.common.Optional instances"
        schemaVisitor.useOptional == true
        then:
        lon.cardinality == MCardinality.OPTIONAL
        lon.type.name == 'Double'
        when: "simpleType enumeration attribute"
        MProperty fix = wptClass.fields['fix']
        MEnum fixTypeEnum = module.lookupClass('FixTypeEnum')
        then: "map to generated Java enum"
        fix != null
        fix.type == fixTypeEnum
        fixTypeEnum != null
        fixTypeEnum.enumValues.size() == 3
        fixTypeEnum.enumNames.size() == 3
        when: "fixed element"
        MProperty author = wptClass.fields['author']
        then:
        author != null
        author.cardinality == MCardinality.REQUIRED
        author.type.name == 'String'
        author.isFinal()
        author.val == 'the dude'
        when: "optional element with default value"
        MProperty ele = wptClass.fields['ele']
        then:
        ele != null
        ele.cardinality == MCardinality.OPTIONAL
        ele.type.name == 'Double' //'java.math.BigDecimal'
        ele.val == '1.0'
        when: "optional dateTime element"
        MProperty time = wptClass.fields['time']
        then:
        time != null
        time.cardinality == MCardinality.OPTIONAL
        time.type.name == 'java.time.LocalDateTime'
        when: "required element"
        MProperty name = wptClass.fields['name']
        then:
        name != null
        name.cardinality == MCardinality.REQUIRED
        name.type.name == 'String'
        when: "unbounded element"
        MProperty places = wptClass.fields['places']
        then:
        places != null
        places.cardinality == MCardinality.LIST
        places.type.name == 'String'
        when: "union of enummerations"
        MEnum measureTypeEnum = module.lookupClass('MeasureTypeEnum')
        then: "union of SimpeType enumerations"
        measureTypeEnum != null
        measureTypeEnum.enumValues.size() == 5
        measureTypeEnum.enumNames.size() == 5
        when: "element from union of enummerations"
        MProperty measure = wptClass.fields['measure']
        then:
        measure != null
        measure.cardinality == MCardinality.REQUIRED
        measure.type == measureTypeEnum
    }
}
