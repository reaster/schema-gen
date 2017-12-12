package com.javagen.schema.common

import com.javagen.schema.model.MRestriction
import com.javagen.schema.xml.node.Restriction
import spock.lang.Specification
import static com.javagen.schema.model.MRestriction.Type
import static com.javagen.schema.model.MRestriction.findValue
import static com.javagen.schema.xml.node.Restriction.RType.*

class MappingUtilSpec extends Specification
{
    def testRestrictoinSet()
    {
        given: "set of XML Schema restritions"
        def i = [new Restriction(enumeration,'one'),
                   new Restriction(minInclusive,'45'),
                   new Restriction(maxInclusive,'45'),
                   new Restriction(minExclusive,'45.5'),
                   new Restriction(maxExclusive,'45.5'),
                   new Restriction(pattern,'[0-9]{1,4}-[0-9]{3,4}-[0-9]{3,4}'),
                   new Restriction(whiteSpace,''),
                   new Restriction(length,'100'),
                   new Restriction(minLength,'10'),
                   new Restriction(maxLength,'20'),
                   new Restriction(fractionDigits,'6'),
                   new Restriction(totalDigits,'9')
        ]
        when: "translated to model restrictions"
        List<MRestriction> o = MappingUtil.translate(i)
        then:
        o.size() >= 9
        findValue(o,Type.enummeration) == 'one'
        findValue(o,Type.min) == 45
        findValue(o,Type.minExclusive) == 45.5
        findValue(o,Type.max) == 45
        findValue(o,Type.maxExclusive) == 45.5
        findValue(o,Type.regexp) == '[0-9]{1,4}-[0-9]{3,4}-[0-9]{3,4}'
        findValue(o,Type.length) == 100
    }

}
