package com.javagen.schema.kotlin

import com.javagen.schema.common.Gen
import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.java.SchemaToJava
import com.javagen.schema.model.MBind
import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MMethod
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry
import com.javagen.schema.xml.XmlSchemaNormalizer
import com.javagen.schema.xml.node.Any
import com.javagen.schema.xml.node.Body
import com.javagen.schema.common.GlobalFunctionsUtil

import static com.javagen.schema.model.MMethod.Stereotype.constructor
import static com.javagen.schema.model.MMethod.Stereotype.equals
import static com.javagen.schema.model.MMethod.Stereotype.getter
import static com.javagen.schema.model.MMethod.Stereotype.hash
import static com.javagen.schema.model.MMethod.Stereotype.setter
import static com.javagen.schema.model.MMethod.Stereotype.toString
import static com.javagen.schema.xml.node.Schema.DEFAULT_NS
import static com.javagen.schema.common.GlobalFunctionsUtil.lowerCase
import static com.javagen.schema.common.GlobalFunctionsUtil.upperCase

class KotlinGen extends SchemaToJava
{
    String sourceFileName = null

    @Override void optionalToPrimitiveWrapper(MProperty property) {}
    @Override MEnum javaEnum(MEnum enumClass)
    {
        List<String> enumValues = enumClass.enumValues.sort()
        def enumNames = []
        def enumNameSet = [] as Set //look for and fix duplicate names
        for(tag in enumValues) {
            def enumName = enumNameFunction.apply(tag)
            int i = 1
            while (enumNameSet.contains(enumName)) //name not unique?
                enumName += i
            enumNameSet << enumName
            enumNames << enumName
        }
        enumClass.enumNames = enumNames
        enumClass.enumValues = enumValues
        //setup a private value addField
        enumClass.addField( new MProperty(name: enumValueFieldName, scope: 'private', 'final': true) )
        enumClass
    }
    @Override def visit(Body body)
    {
        println "body @type=${body.type} @mixed=${body.mixedContent}"
        if (body.mixedContent) println "WARNING: mixed content currently not supported for body: ${body}"
        MCardinality container = container(body)
        String name = propertyNameFunction.apply(bodyPropertyName)
        String type = schemaTypeToPropertyType(body.type ?: schema.getGlobal(DEFAULT_NS, 'string'), container)

        MProperty property = new MProperty(name:name, type:type, cardinality: container, attr: ['body':name])
        MClass clazz = nestedStack.peek()
        clazz.addField(property)
        clazz.addMethod(new MMethod(stereotype: constructor, params:[new MBind(name: name, type: type)], body: "" ))
        callback.gen(body, property)
    }

    MProperty genAny(String propertyName, Any any)
    {
        MClass clazz = nestedStack.peek()
        MCardinality container = container(any)
        MType type = schemaTypeToPropertyType(any.type ?: schema.getGlobal(DEFAULT_NS, anyType), container)
        MProperty property
        if (container == MCardinality.LIST) {
            /*
            class Extensions(@JsonIgnore var map:LinkedHashMap<String, String> = linkedMapOf())
            {
            public @JsonAnySetter fun set(key: String, value: String) = this.map.put(key, value)
            public @JsonAnyGetter fun all(): Map<String, String> = this.map
            override fun hashCode() = map.hashCode()
            override fun equals(other: Any?) = when {
            this === other -> true
            other is Extensions -> map.size == other.map.size
                && map.all { (k,v) -> v.equals(other.map[k]) }
            else -> false
            }
            override fun toString() = map.toString()
            }
             */

            String anyClassName = upperCase(propertyName)
            MClass anyClass = (MClass)MType.lookupType(anyClassName)
            if (!anyClass || anyClass.fields.isEmpty()) { //TODO should this be done in the ComplexType method?
                anyClass = new MClass(name: anyClassName)
                MBind keyParam = new MBind(name: 'key', type: 'String')
                MBind valParam = new MBind(name: 'value', type: type)
                MBind anyParam = new MBind(name: 'other', type: 'Any', cardinality: MCardinality.OPTIONAL)
                MProperty anyProp = new MProperty(name:'map', type:type, cardinality:MCardinality.LINKEDMAP, val:'linkedMapOf()', attr:['keyType':'String'])
                anyClass.addField( anyProp )
                anyClass.addMethod(new MMethod(name: 'set', params: [keyParam, valParam], expr: 'this.map.put(key, value)', stereotype: setter))
                anyClass.addMethod(new MMethod(name: 'all', type: anyProp, expr: 'this.map', stereotype: getter))
                anyClass.addMethod(new MMethod(name: 'hashCode', override: true, expr: 'map.hashCode()', stereotype: hash))
                anyClass.addMethod(new MMethod(name: 'equals', override: true, params: [anyParam], expr: this.&mapHashCodeMethodBody, stereotype: equals))
                anyClass.addMethod(new MMethod(name: 'toString', override: true, expr: 'map.toString()', stereotype: toString))
                clazz.parentModule().addClass(anyClass)
                MType.registerType(anyClass)
                callback.gen(any, anyClass)
            }
            property = new MProperty(name:propertyName, type:anyClass, cardinality:MCardinality.OPTIONAL, final:any.fixed!=null)

        } else {
            String val = any.fixed ?: any.'default'
            property = new MProperty(name:propertyName, type:type, cardinality:container, final:any.fixed!=null, val:val)
        }
        clazz.addField(property)
        callback.gen(any, property)
        property
    }

    def mapHashCodeMethodBody(MMethod m, CodeEmitter v)
    {
        v.out << 'when {'
        v.next()
        v.out << '\n' << v.tabs << 'this === other -> true'
        v.out << '\n' << v.tabs << 'other is Extensions -> map.size == other.map.size'
        v.next()
        v.out << '\n' << v.tabs << '&& map.all { (k,v) -> v.equals(other.map[k]) }'
        v.previous()
        v.out << '\n' << v.tabs << 'else -> false'
        v.previous()
        v.out << '\n' << v.tabs << '}'
    }

    KotlinGen()
    {
        super(true)
        fileExtension = 'kt'
        simpleXmlTypeToPropertyType = { typeName ->
            KotlinTypeRegistry.simpleXmlTypeToPropertyType[typeName]
        }
        if ( ! MTypeRegistry.isInitialized() )
            new KotlinTypeRegistry()
        callback = new KotlinJacksonCallback(this)
        pipeline = [
                //new PreJavaVisitor(xml: this),
                new KotlinEmitter(gen: this)
        ]
        classOutputFile = { gen,clazz -> Gen.pathFromSourceFileName(gen, clazz) } //combine classes into single source file
        //TODO convert to Kotlin:
        enumNameFunction = { text -> GlobalFunctionsUtil.javaEnumName(text, false) }
        propertyNameFunction = { text -> GlobalFunctionsUtil.legalJavaName(lowerCase(text)) }
        constantNameFunction = { text -> GlobalFunctionsUtil.javaConstName(text) }

        //kotlin-gpx
//        srcDir = new File('../schema-gen-examples/kotlin-gpx/src/main/kotlin-gen')
//        schemaFile = new File('../schema-gen-examples/kotlin-gpx/src/main/resources/gpx.xsd').toURI().toURL()

        //kotlin-hsf
//        schemaFile = new File('/Users/richard/dev/hs/hsf-data/hsf-1_1.xsd').toURI().toURL()
//        srcDir = new File('../schema-gen-hsf/hsf-kotlin/src/main/kotlin-gen')
//        customPluralMappings = ['hours':'hours'] //needed for irregular nouns: tooth->teeth, person->people
//        def enumCustomNames = ['primitive+':'PrimitivePlus','$':'Cheap','$$':'Moderate','$$$':'Pricy','$$$$':'Exclusive']
//        def unknownEnum = 'Unknown'
//        enumNameFunction = { text -> text.contains('?') ? unknownEnum : enumCustomNames[text] ?: GlobalFunctionsUtil.swiftEnumName(text, false) }

        useOptional = true
    }

    @Override def gen()
    {
        schema = new XmlSchemaNormalizer().buildSchema(schemaFile)
        visit(schema)
        MModule rootModule = getModel()
        if (!sourceFileName) //if no source file name defined, use first root element name
            sourceFileName = schema.rootElements.isEmpty() ? 'JavaGen' : upperCase(schema.rootElements.first().name)
        rootModule.sourceFile = Gen.pathFromSourceFileName(this, rootModule, sourceFileName)
        pipeline.each { visitor ->
            visitor.visit(rootModule)
        }
    }

    static void main(String[] args) {
        new KotlinGen().gen()
    }

}
