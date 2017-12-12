package com.javagen.schema.xml.node

import com.javagen.schema.xml.QName
import groovy.transform.ToString

@ToString(includePackage=false)
abstract class Node
{
    QName qname
    String id
    boolean isRoot() { return qname!=null }
    void setQname(String name) { this.qname = new QName(name:name) }
    void setQname(QName qname) { this.qname = qname }
}
