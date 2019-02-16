/*
 * Copyright (c) 2017 Outsource Cafe, Inc.
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

import com.javagen.schema.xml.node.*

/**
 * Traverses the nodes of a Schema object graph. Subclass overridden methods will usually call these methods
 * for navigating the tree using trait syntax: SchemaVisitor.super.visit(xml).
 *
 * @author Richard Easterling
 */
trait XmlSchemaVisitor
{
    boolean printTrace = false

    def visit(Schema schema) {
        if (printTrace) println "xml @targetNamespace=${schema.prefixToNamespaceMap[Schema.targetNamespace]}"
        for (Type type : schema.globalTypes.values()) {
            if (!type.builtInType) {
                //if (type.qname.name == 'extensionsType')
                //    println 'extensionsType'
                visit(type)
            }
        }
        for (AttributeGroup ag : schema.globalAttributeGroups.values()) {
            visit(ag)
        }
        for (Group group : schema.globalGroups.values()) {
            visit(group)
        }
        for (QName rootNode : schema.rootElements) {
            visit(rootNode)
        }
    }

//    def visit(Annotation annotation) {
//        if (printTrace) println "annotation ${annotation.documentation?.first()?.text}"
//        annotation.documentation.each { visit(it) }
//        annotation.attr.each { visit(it) }
//    }


    def visit(Any any) {
        if (printTrace) println "any @name=${any.qname.name}"
    }

    def visit(AnyAttribute anyAttribute) {
        if (printTrace) println "anyAttribute @name=${anyAttribute.qname.name}"
    }

    def visit(Attribute attribute) {
        if (printTrace) println "attribute @name=${attribute.qname.name} @type=${attribute.type}"
    }

    def visit(AttributeGroup attributeGroup) {
        if (printTrace) println "attributeGroup @name=${attributeGroup.qname.name}"
        for (Attribute attribute : attributeGroup.attributes) {
            visit(attribute)
        }

    }

    def visit(Element element) {
        if (printTrace) println "element @name=${element.qname.name} @type=${element.type}"
    }

    def visit(Body body) {
        if (printTrace) println "body @type=${body.type} @mixed=${body.mixedContent}"
    }

    def visit(ComplexType complexType) {
        if (printTrace) println "complexType @name=${complexType.qname.name}"
        //if (complexType.annotation) visit(complexType.annotation)
        for (Attribute attribute : complexType.attributes) {
            visit(attribute)
        }
        for (Element element : complexType.elements) {
            visit(element)
        }
        for (Compositor compositor : complexType.compositors) {
            visit(compositor)
        }
        if (complexType.isBody()) {
            visit(complexType.getBody())
        }
    }

    def visit(All all) {
        if (printTrace) println "all"
        for (Element element : all.elements) {
            visit(element)
        }
    }

    def visit(Choice choice) {
        if (printTrace) println "choice @minOccurs=${choice.minOccurs} @maxOccurs=${choice.maxOccurs}"
        for (Element element : choice.elements) {
            visit(element)
        }
        for (Group group : choice.groups) {
            visit(group)
        }
        for (Compositor compositor : choice.compositors) {
            visit(compositor)
        }
    }

    def visit(Sequence sequence) {
        if (printTrace) println "sequence @minOccurs=${sequence.minOccurs} @maxOccurs=${sequence.maxOccurs}"
        for (Element element : sequence.elements) {
            visit(element)
        }
        for (Group group : sequence.groups) {
            visit(group)
        }
        for (Compositor compositor : sequence.compositors) {
            visit(compositor)
        }
    }

    def visit(Group group) {
        if (printTrace) println "group @name=${group.qname.name}"
        for (Element element : group.elements) {
            visit(element)
        }
        for (Group subgroup : group.groups) {
            visit(subgroup)
        }
        for (Compositor compositor : group.compositors) {
            visit(compositor)
        }
    }

    def visit(List list) {
        if (printTrace) println "list @itemType=${list.itemType}"
    }

    def visit(SimpleType simpleType) {
        if (printTrace) println "simpleType @name=${simpleType.qname.name}"
        for (Attribute attribute : simpleType.attributes) {
            visit(attribute)
        }
        boolean bodyWithBuiltInType = !simpleType.isInheritedBaseType()
//        if (simpleType.qname.name == 'mediaReferenceType' || simpleType.qname.name == 'phoneNumberType')
//            println simpleType.qname.name
        if (bodyWithBuiltInType) {
            visit(simpleType.getBody())
        }
    }

    def visit(TextOnlyType textOnlyType) {
        if (printTrace) println "textOnlyType @name=${textOnlyType.qname}"
    }

    def visit(Union union) {
        if (printTrace) print "union @name=${union.qname.name}"
        union.simpleTypes.eachWithIndex { SimpleType type, int i ->
            if (printTrace) print "${i == 0 ? ':' : ','} ${type.qname?.name}"
        }
        if (printTrace) println()
    }

    def visit(QName root) {
        if (printTrace) println "root node: ${root}"
    }

    /** visit global nodes without visiting sub-nodes, useful for creating proxy classes for type reference lookups */
    def preVisit(Schema schema) {
        if (printTrace) println "preVisit global types=${schema.globalTypes.values().size()}, AttributeGroups: ${schema.globalAttributeGroups.values().size()}, Groups: ${schema.globalGroups.values().size()}"
        for (Type type : schema.globalTypes.values()) {
            if (!type.builtInType) {
                preVisit(type)
            }
        }
        for (AttributeGroup ag : schema.globalAttributeGroups.values()) {
            preVisit(ag)
        }
        for (Group group : schema.globalGroups.values()) {
            preVisit(group)
        }
        for (Attribute attribute : schema.attributes) {
            preVisit(attribute)
        }
        for (Element element : schema.elements) {
            preVisit(element)
        }
    }
    def preVisit(AttributeGroup attributeGroup) { }
    def preVisit(Group group) { }
    def preVisit(TextOnlyType textOnlyType) { }
    def preVisit(SimpleType simpleType) { }
    def preVisit(ComplexType complexType) { }
    def preVisit(Attribute attribute) { }
    def preVisit(Element element) { }
}