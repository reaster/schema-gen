package com.javagen.schema.kotlin

import spock.lang.Shared

import static com.javagen.schema.xml.node.Schema.getDEFAULT_NS

class SchemaToKotlinSpec {
    @Shared def prefixToNamespaceMap = ['xsd':DEFAULT_NS, 'gpx':'http://www.topografix.com/GPX/1/1', targetNamespace:'http://www.topografix.com/GPX/1/1']

}
