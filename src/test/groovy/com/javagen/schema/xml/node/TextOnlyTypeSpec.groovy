package com.javagen.schema.xml.node

import spock.lang.Shared
import spock.lang.Specification

class TextOnlyTypeSpec extends Specification
{
    @Shared Schema schema = new Schema()

    def testRestrictoinSet()
    {
        when:
        TextOnlyType t = new TextOnlyType(restrictions: [new Restriction('enumeration','one'),new Restriction('enumeration','two')])
        then:
        t.restrictionSet() == EnumSet.of(Restriction.RType.enumeration)
        when:
        t = new TextOnlyType(restrictions: [new Restriction('enumeration','one'),new Restriction('length','2')])
        then:
        t.restrictionSet() == EnumSet.of(Restriction.RType.enumeration,Restriction.RType.length)
    }

    def test_isEmpty()
    {
        when: "element is a TextOnlyType "
        TextOnlyType t = new TextOnlyType()
        then: "is always NOT empty"
        t.isEmpty() == false
        when: "element is a SimpleType"
        SimpleType s = new SimpleType()
        then: "is empty if no base type"
        s.isEmpty() == true
        when: "SimpleType has a base"
        s = new SimpleType(base: schema.getXsdType('string'))
        then: "it is NOT empty"
        s.isEmpty() == false
    }

}
