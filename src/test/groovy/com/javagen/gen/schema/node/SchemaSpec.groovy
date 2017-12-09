package com.javagen.gen.schema.node

import spock.lang.*
import com.javagen.gen.schema.QName

class SchemaSpec extends Specification
{

    def testMap()
    {
        given:
        Map<String,String> prefixToNamespaceMap = ['xsd':Schema.DEFAULT_NS, 'gpx':'http://www.topografix.com/GPX/1/1']
        when:
        def ns = prefixToNamespaceMap['xsd']
        then:
        ns == Schema.DEFAULT_NS
        when:
        ns = prefixToNamespaceMap['gpx']
        then:
        ns == 'http://www.topografix.com/GPX/1/1'

    }
    def testXmlParsing()
    {
        given: "an initilized Schema instance"

        Schema schema = new Schema()

        when: "requesting simple type"
        for(def e : schema.globalTypes)
            println "key:${e.key}->${e.value}"
        def qname = new QName(namespace:Schema.DEFAULT_NS, name:'boolean')
        def boolType = schema.getGlobal(qname)
        println boolType

        then: "confirm simple type is defined"
        boolType != null
        boolType.qname == qname
        boolType.qname.name == 'boolean'
    }

}
