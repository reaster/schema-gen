/*
 * Copyright (c) 2019 Outsource Cafe, Inc.
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

package com.javagen.schema.java

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.IntNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule
import com.javagen.schema.model.MModule
import com.javagen.schema.xml.XmlSchemaNormalizer
import com.javagen.schema.xml.node.Schema
import spock.lang.Specification

import javax.xml.bind.annotation.XmlElement
import javax.xml.bind.annotation.XmlElements
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter
import java.lang.reflect.Constructor
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.stream.Collectors

/**
 * Experiments with supporting both JSON and XML polymorphic collections to support the choice-based element types.
 *
 * https://stackoverflow.com/questions/50787107/error-deserializing-xml-using-jackson-to-generated-choice-from-xsd
 * http://federico.defaveri.org/2016/11/20/handling-polymorphism-with-jackson/
 * https://www.baeldung.com/jackson-inheritance
 * https://www.baeldung.com/jackson-custom-serialization
 * http://tutorials.jenkov.com/java-json/jackson-objectmapper.html
 * https://stackoverflow.com/questions/29676933/handle-polymorphic-with-stddeserializer-jackson-2-5
 */
class PolymorphicSpec extends Specification
{
//    public static class Foo {
//
//        @JacksonXmlElementWrapper(useWrapping = false)
//        @XmlElements(value = {
//            @XmlElement(name = "A", type = Integer.class) @XmlElement(name = "B", type = Float.class)
//        })
//        public List items;
//    }

    @JacksonXmlRootElement(localName = "poly")
    //@JsonSerialize(using = PolymorphicXmlSerializer.class)
    //@JsonDeserialize(using = PolymDeserializer.class)
    static class Poly {

        @XmlElements([
            @XmlElement(name = "a", type = String.class),
            @XmlElement(name = "b", type = Double.class)
        ])
        public List<Object> items;

        public Poly(List<Object> items) {
            this.items = items;
        }

        public Poly() {
            this(new ArrayList<>());
        }
        public void addToList(Object element) {
            items.add(element);
        }
    }

    static class PolymorphicXmlSerializer<T> extends StdSerializer<T>
    {
        private final Map<T,String> tagMap
        private Field collection

        protected Map<T,String> getTagMap(Class<T> type)
        {
            Map<T,String> results = new HashMap<>()
            collection = Arrays.asList(type.fields).stream().find{
                f -> f.getAnnotation(XmlElements)
            }
            if (collection!=null) {
                XmlElements xmlElements = collection.getAnnotation(XmlElements)
                for(XmlElement e : xmlElements.value()) {
                    results.put(e.type(),e.name())
                }
            } else {
//                JsonSubTypes anno = type.getDeclaredAnnotation(JsonSubTypes)// ?: type.getAnnotatedSuperclass(JsonSubTypes)
//                if (anno==null)
                    throw new IllegalStateException("expected @XmlElements or @JsonSubTypes polymorphic annotation")
//                 for(JsonSubTypes.Type e : anno.value()) {
//                    results.put(e.value(),e.name())
//                }
            }
            results
        }

        public PolymorphicXmlSerializer(T t) {
            super(t);
            this.tagMap = getTagMap(_handledType)
        }

        @Override
        public void serialize(T value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            Collection list = (Collection)collection.get(value);
            for(Object val : list) {
                String tag = tagMap.get(val.getClass())
                if (tag == null) {
                    throw new IllegalStateException("expected: ${Arrays.toString(tagMap.keySet().toArray())}, not ${val.class.simpleName} instance: '${val}");
                }
                jgen.writeObjectField(tag, val);
            }
        }
    }

    static class PolymorphicXmlDeserializer<T> extends StdDeserializer<T>
    {
        private final String discriminator;
        private final Map<String,T> typeMap

        private Map<String, T> getTypeMap()
        {
            Map<T,String> tagMap = new HashMap<>()
            Field f = Arrays.asList(_valueClass.fields).stream().find{
                f -> f.getAnnotation(XmlElements)
            }
            if (f!=null) {
                XmlElements xmlElements = f.getAnnotation(XmlElements)
                for(XmlElement e : xmlElements.value()) {
                    tagMap.put(e.type(),e.name())
                }
            } else {
                throw new IllegalStateException("expected @XmlElements or @JsonSubTypes polymorphic annotation")
            }
            //Map<String,T> tagMap = PolymorphicXmlSerializer.getTagMap(_valueClass)
            //def results = tagMap.entrySet().stream().map( e -> e.getValue(), e -> e.getKey()).collect(Collectors.toMap());
            def results = tagMap.collectEntries { e -> [(e.value): e.key] }
            results
        }

        public PolymorphicXmlDeserializer() {
            this(null);
        }

        public PolymorphicXmlDeserializer(T t) {
            super(t);
            this.typeMap = getTypeMap()
            this.discriminator = "name"
        }

        public PolymorphicXmlDeserializer(T t, Map<String,T> typeMap, String discriminator) {
            super(t);
            this.typeMap = typeMap;
            this.discriminator = discriminator;
        }

        public PolymorphicXmlDeserializer(T t, Map<String,T> typeMap) {
            this(t, typeMap, "name");
        }


        @Override
        public T deserialize(JsonParser parser, DeserializationContext ctx) throws IOException, JsonProcessingException
        {
            JavaType javaType = ctx.constructType(Poly.class)
            if (javaType.hasGenericTypes())
                println 'true'
            List<Object> result = new ArrayList<>();
            ObjectMapper mapper = (ObjectMapper)parser.getCodec();
            boolean loop = !parser.isClosed()
            while(loop) {
                JsonToken jsonToken = parser.nextToken();
                switch (jsonToken) {
                    case JsonToken.FIELD_NAME:
                        String fieldName = parser.getCurrentName();
                        System.out.println(fieldName);
                        jsonToken = parser.nextToken();
                        Class<?> itemClass = typeMap.get(fieldName)
                        if (itemClass == null) {
                            throw new IllegalStateException("XML element expected: ${Arrays.toString(typeMap.keySet().toArray())}, not: '${fieldName}'");
                        }
                        Object parsed = mapper.readValue(parser, itemClass);
                        if (parsed == null) {
                            throw new IllegalStateException("'${fieldName}' key has no value, expecting instance of: ${Arrays.toString(typeMap.values().toArray())}");
                        }
                        result.add(parsed);
                        break
                    case JsonToken.END_OBJECT:
                        loop = false
                        break
                }
                if (parser.isClosed())
                    loop = false
            }
            Constructor<T> constructor = _valueClass.getConstructor(List.class)
            return constructor.newInstance(result)
        }
    }

    def "test Poly Jackson XML polymorphics"() {
        given:
        def xml = """<poly><a>text</a><b>4.5</b></poly>"""
        Poly poly = new Poly()
        poly.addToList('text')
        poly.addToList((Double)4.5)

        SimpleModule m = new SimpleModule("module", new Version(1,0,0,null,null,null));
        m.addDeserializer(Poly.class, new PolymorphicXmlDeserializer<Poly>(Poly.class));
        m.addSerializer(Poly.class, new PolymorphicXmlSerializer<Poly>(Poly.class));
        XmlMapper xmlMapper = new XmlMapper()
        xmlMapper.registerModule(m)

        when: "try unmarshalling - NOT what we want!"
        String xml3 = xmlMapper.writeValueAsString(poly)
        then:
        System.out.println(xml3)
        xml == xml3
        //'<poly><items><items>text</items><items>4.5</items></items></poly>' == xml3

        when: "try marshalling"
        Poly poly2 = xmlMapper.readValue(xml, Poly)
        then:
        System.out.println(poly2)
        poly2.items.size() == 2
        poly2.items[0] == 'text'
        poly2.items[1] == new Double(4.5)
    }

    @JsonTypeInfo(use= JsonTypeInfo.Id.NAME)
    @JsonSubTypes([
            @JsonSubTypes.Type(value=Cat.class, name = "cat"),
            @JsonSubTypes.Type(value=Dog.class, name = "dog")
    ])
    static abstract class AnimalJsonMixIn {}

    static abstract class Animal {
    }
    static class Dog extends Animal {
        int sticksFetched = 3481
        String toString() { "Doggy" }
    }
    static class Cat extends Animal {
        int birdKills = 5
        String toString() { "Kitty" }
    }
    @JacksonXmlRootElement(localName = "farm")
    static class AnimalFarm {
        @XmlElements([
                @XmlElement(name = "cat", type = Cat.class),
                @XmlElement(name = "dog", type = Dog.class)
        ])
        public List<Animal> animals
        AnimalFarm() { this(new ArrayList<>()) }
        AnimalFarm(List<Animal> animals) { this.animals = animals }
    }

    def "test Animal Jackson XML polymorphics"() {
        given:
        SimpleModule m = new SimpleModule("module", new Version(1,0,0,null,null,null));
        m.addDeserializer(AnimalFarm.class, new PolymorphicXmlDeserializer<AnimalFarm>(AnimalFarm.class));
        m.addSerializer(AnimalFarm.class, new PolymorphicXmlSerializer<AnimalFarm>(AnimalFarm.class));
        XmlMapper xmlMapper = new XmlMapper()
        xmlMapper.registerModule(m)

        when: "try unmarshalling polymorphic list"
        AnimalFarm farm = new AnimalFarm()
        farm.animals << new Dog()
        farm.animals << new Cat()
        farm.animals << new Dog()
        String xml4 = xmlMapper.writeValueAsString(farm)
        then:
        System.out.println(xml4)
        xml4.contains('cat')

        when: "try marshalling animals"
        AnimalFarm farm2 = xmlMapper.readValue("""<farm><dog><sticksFetched>3481</sticksFetched></dog><cat><birdKills>5</birdKills></cat><dog><sticksFetched>3481</sticksFetched></dog></farm>""", AnimalFarm)
        then:
        System.out.println(farm2)
        farm2 instanceof AnimalFarm
        farm2.animals.size() == 3
    }



    def "test Animal Jackson JSON polymorphics"() {
        given:
        ObjectMapper jsonMapper = new ObjectMapper()
        jsonMapper.addMixIn(Animal, AnimalJsonMixIn)

        when: "try unmarshalling cat"
        String json2 = jsonMapper.writeValueAsString(new Cat())
        then:
        System.out.println(json2)
        json2.contains('cat')
        when: "try unmarshalling dog"
        String json3 = jsonMapper.writeValueAsString(new Dog())
        then:
        System.out.println(json3)
        json3.contains('dog')

        when: "try unmarshalling polymorphic list"
        AnimalFarm farm = new AnimalFarm()
        farm.animals << new Dog()
        farm.animals << new Cat()
        farm.animals << new Dog()
        String json4 = jsonMapper.writeValueAsString(farm)
        then:
        System.out.println(json4)
        json4.contains('cat')


        when: "try marshalling cat"
        Animal cat2 = jsonMapper.readValue("""{ "@type" : "cat"}""", Animal)
        then:
        System.out.println(cat2)
        cat2 instanceof Cat
        when: "try marshalling dog"
        Animal dog2 = jsonMapper.readValue("""{ "@type" : "dog"}""", Animal)
        then:
        System.out.println(dog2)
        dog2 instanceof Dog
        when: "try marshalling animals"
        AnimalFarm farm2 = jsonMapper.readValue("""{"animals":[{"@type":"dog"},{"@type":"cat"},{"@type":"dog"}]}""", AnimalFarm)
        then:
        System.out.println(farm2)
        farm2 instanceof AnimalFarm
        farm2.animals.size() == 3
    }
}
