package com.javagen.gen.schema.node

import spock.lang.Specification

class TextOnlyTypeSpec extends Specification
{
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

}
