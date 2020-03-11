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

package com.javagen.schema.xml

import com.javagen.schema.java.JavaGen
import com.javagen.schema.xml.node.Choice
import com.javagen.schema.xml.node.ComplexType
import com.javagen.schema.xml.node.Element
import com.javagen.schema.xml.node.Schema
import com.javagen.schema.xml.node.Type
import spock.lang.Ignore
import spock.lang.See
import spock.lang.Specification

@See(["https://www.w3.org/TR/xmlschema-0/", "https://www.youtube.com/watch?v=vfUKKCqIlY8"])
class XmlSchemaNormalizerSpec extends Specification
{
    def "test multi file namespace handling"() {
        given:
        Schema s = new XmlSchemaNormalizer().buildSchema(new File('src/test/resources/fo.xsd').toURI().toURL())
        //println s //uncomment to see schema model we're validating
        when: "element assigned"
        Element fee = s.lookupElement(new QName(namespace:'http://javagen.com/fee', name:'fee'))
        Element fo = s.lookupElement(new QName(namespace:'http://javagen.com/fo', name:'fo'))
        then: "check expected values"
        fee != null
        fo != null
        fee.type == s.getGlobal('http://javagen.com/fee','FeeType')
        fo.type == s.getGlobal('http://javagen.com/fo','FoType')
    }


    def "test basic normalizer API"() {
        given:
        def xml = """<xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:j="http://joe.org/schemmata" targetNamespace="http://joe.org/schemmata" elementFormDefault="qualified">
                        <xsd:element name="joe" type="j:JoeType" />
                        <xsd:complexType name="JoeType">
                            <xsd:choice minOccurs="0" maxOccurs="unbounded">
                                <xsd:element name="name" type="xsd:string"/>
                                <xsd:element name="zip" type="xsd:positiveInteger" />
                            </xsd:choice>
                        </xsd:complexType>
                    </xsd:schema>"""
        Schema s = new XmlSchemaNormalizer().buildSchema(xml)
        //println s //uncomment to see schema model we're validating
        /*
            Schema[
              prefixToNamespaceMap[
                xml -> http://www.w3.org/XML/1998/namespace
                xsd -> http://www.w3.org/2001/XMLSchema
                 -> http://joe.org/schemmata
                targetNamespace -> http://joe.org/schemmata
              ]
              elements (global elements) [
                Element(empty:false, _abstract:false, nillable:false, minOccurs:1, maxOccurs:1, super:Value(ComplexType(ComplexType[name=JoeType base=null]), Node(joe:http://joe.org/schemmata, )))
              ]
              attributes (global attributes) [
              ]
              globalAttributeGroups[
              ]
              globalGroups[
              ]
              globalTypes[
                anyType:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=anyType base=null]
                anySimpleType:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=anySimpleType base=anyType]
                string:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=string base=anySimpleType]
                decimal:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=decimal base=anySimpleType]
                boolean:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=boolean base=anySimpleType]
                float:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=float base=anySimpleType]
                double:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=double base=anySimpleType]
                duration:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=duration base=anySimpleType]
                dateTime:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=dateTime base=anySimpleType]
                time:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=time base=anySimpleType]
                date:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=date base=anySimpleType]
                gYearMonth:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=gYearMonth base=anySimpleType]
                gYear:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=gYear base=anySimpleType]
                gMonthDay:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=gMonthDay base=anySimpleType]
                gDay:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=gDay base=anySimpleType]
                gMonth:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=gMonth base=anySimpleType]
                hexBinary:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=hexBinary base=anySimpleType]
                base64Binary:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=base64Binary base=anySimpleType]
                anyURI:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=anyURI base=anySimpleType]
                QName:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=QName base=anySimpleType]
                NOTATION:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=NOTATION base=anySimpleType]
                integer:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=integer base=decimal]
                nonPositiveInteger:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=nonPositiveInteger base=integer]
                nonNegativeInteger:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=nonNegativeInteger base=integer]
                long:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=long base=integer]
                int:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=int base=long]
                short:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=short base=int]
                byte:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=byte base=short]
                negativeInteger:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=negativeInteger base=nonPositiveInteger]
                positiveInteger:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=positiveInteger base=nonNegativeInteger]
                unsignedLong:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=unsignedLong base=nonNegativeInteger]
                unsignedInt:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=unsignedInt base=unsignedLong]
                unsignedShort:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=unsignedShort base=unsignedInt]
                unsignedByte:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=unsignedByte base=unsignedShort]
                normalizedString:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=normalizedString base=null]
                token:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=token base=normalizedString]
                language:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=language base=token]
                Name:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=Name base=token]
                NMTOKEN:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=NMTOKEN base=token]
                NMTOKENS:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=NMTOKENS base=NMTOKEN]
                NCName:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=NCName base=Name]
                ID:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=ID base=NCName]
                IDREF:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=IDREF base=NCName]
                IDREFS:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=IDREFS base=IDREF]
                ENTITY:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=ENTITY base=NCName]
                ENTITIES:http://www.w3.org/2001/XMLSchema -> TextOnlyType[name=ENTITIES base=ENTITY]
                JoeType:http://joe.org/schemmata -> ComplexType(ComplexType[name=JoeType base=null])
              ]
              rootElements (qname references) [
                joe:http://joe.org/schemmata
              ]
            ] */
        expect: "one global element"
        s.elementCount() == 1
        s.globalTypes.size() > 0
        when: "element assigned"
        Element e = s.childElements()[0]
        then: "check expected values"
        e.minOccurs == 1
        e.maxOccurs == 1
        e.qname.name == 'joe'
        e.qname.namespace == 'http://joe.org/schemmata'
        e.body == false
        e.mixed == false
        e.abstract == false
        e.empty == false
        e.nillable == false
        when: 'JoeType retreived'
        ComplexType t = s.getGlobal('http://joe.org/schemmata','JoeType')
        then: "check expected values"
        t == e.type
        t == s.getGlobal(new QName(namespace:'http://joe.org/schemmata', name:'JoeType'))
        t == s.getGlobal('JoeType') //default namespace
        t.compositors.size() == 1
        t.compositors[0] instanceof Choice
        when: 'Choice assigned'
        Choice c = t.compositors[0]
        then: "check expected values"
        c.minOccurs == 0
        c.maxOccurs == Integer.MAX_VALUE //unbounded
        c.elementCount() == 2
        c.childElements()[0] instanceof Element
        c.childElements()[1] instanceof Element
        when: 'child elements assigned'
        Element e1 = c.childElements()[0]
        Element e2 = c.childElements()[1]
        then: "check expected values"
        e1.qname.name == 'name'
        e1.type == s.getGlobal('http://www.w3.org/2001/XMLSchema','string')
        e1.minOccurs == 1
        e1.maxOccurs == 1
        e2.qname.name == 'zip'
        e2.type == s.getGlobal('xsd:positiveInteger')
        e2.minOccurs == 1
        e2.maxOccurs == 1
        s.rootElements.size() == 1
        s.rootElements[0] == new QName(namespace: 'http://joe.org/schemmata', name:'joe')
        s.rootElements[0] == e.qname
    }

}