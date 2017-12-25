# schema-gen
schema-gen is a multi-language, interoperable, XML Schema code generator. 
The generated code allows reading, writing, manipulating, and transmitting data in the two most widely used industry formats: XML and JSON. 
Currently supported languages are Java, Kotlin, and Swift 4 and the code is interoperable, meaning it's well suited for developing cross-language, mobile, client-server applications.

Producing concise, readable code is a top schema-gen priority and it takes naming conventions seriously, providing a high level of flexibility and configurability.

This software is written in Groovy and is packaged as a Gradle plugin.

## Currently Supported Languages

### Java
**Features:** Generated POJOs contain [Jackson](https://github.com/FasterXML/jackson-dataformat-xml) annotations supporting reading and writing both JSON and XML documents. XML Schema restrictions are also translated into [Bean Validation 2.0](http://beanvalidation.org/) annotations. Equals, hashCode and toString methods are also generated to facilitate testing. See the [java-gpx](https://github.com/reaster/schema-gen-examples/tree/master/java-gpx) sample project.

**Usage:** The main entry point is [com.javagen.schema.java.JavaGen](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/schema/java/JavaGen.groovy) which can be invoked directly or via the gradle plugin. By default, the generated code is placed in the src/main/java-gen folder to keep it separate from hand-written code.

**Limitations:** Because Java does not have the extension features of more recent languages, it's harder to maintain a clean separation between hand-written and generated code. You may be forced to put your business logic in the generated files with the downside being, if you ever have to regenerate, you'll have to manually merge your code back in.

### Kotlin
**Features:** By leveraging [data classes](https://kotlinlang.org/docs/reference/data-classes.html), Kotlin generated code is very concise. Classes contain [Jackson](https://github.com/FasterXML/jackson-dataformat-xml) annotations supporting reading and writing both JSON and XML documents. XML Schema restrictions are also translated into [Bean Validation 2.0](http://beanvalidation.org/) annotations. Extending generated code with business logic can be acheived using [extensions](https://kotlinlang.org/docs/reference/extensions.html). See the [kotlin-gpx](https://github.com/reaster/schema-gen-examples/tree/master/kotlin-gpx) sample project.

**Usage:** The main entry point is [com.javagen.schema.kotlin.KotlinGen](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/schema/kotlin/KotlinGen.groovy) which can be invoked directly or via the gradle plugin. By default, the generated code is placed in the src/main/kotlin-gen folder to keep it separate from hand-written code.

**Limitations:** The code generator attempts to create no-argument constructors by setting default values on every property, which in some cases can cause problems. To minimize unessasary annotations, the generated code requires Java 8 parameter name support. See the build.gradle and unit tests in [kotlin-gpx](https://github.com/reaster/schema-gen-examples/tree/master/kotlin-gpx) for proper setup and configuration.

### Swift
**Features:** schema-gen generates code utilizing the built-in Encodable and Decodable JSON support introduced in Swift 4. Extending generated code with business logic can be acheived using Extensions. See the [swift-gpx](https://github.com/reaster/schema-gen-examples/tree/master/swift-gpx) sample project.

**Usage:** The main entry point is [com.javagen.schema.swift.SwiftGen](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/schema/swift/SwiftGen.groovy) which can be invoked directly or via the gradle plugin. By default, the generated code is placed in the src/swift-gen folder to keep it separate from hand-written code.

**Limitations:** Swift only supports JSON serialization. Assuming you're server is written in Kotlin or Java, communication with a Swift client can utilize JSON, even if the documents are stored as XML, thanks to Jackson's support of both. However, given a good XMLEncoder, XML support should be straight forward to add. The author wrote [saxy](https://github.com/reaster/saxy) in Objective-C the last time around and it's somebody else's turn to do this for Swift ;-)

## Usage

### Gradle Plugin
schema-gen includes a Gradle Plugin which can be added to your gradle.build file by including it in your buildscript, and applying and configuring it:
  
```
buildscript {
    repositories {
        mavenLocal()
        mavenCentral()
    }
    dependencies {
        classpath 'com.javagen:schema-gen:0.9.0'
    }
}

apply plugin: 'com.javagen.schema-gen'

schemaGen {
    swift {
        schemaURL = new URL('file:src/main/resources/gpx.xsd')
    }
}

```  
To generate code use the gen task:
```
swift-gpx> gradle gen
```
To remove the generated code, use the genClean task:
```
swift-gpx> gradle genClean
```

To install the Gradle plugin locally, use the maven install task:
```
schema-gen> gradle install
```
A good starting point is to download the [schema-gen-examples](https://github.com/reaster/schema-gen-examples) project, regenerate the code and run the tests:
```
schema-gen-examples> gradle genClean gen test
```
### Running the Generators in your IDE

For code generator development work, you'll want to run the target language gen class (JavaGen, KotlinGen, SwiftGen) directly.  If you place your project (say, kotlin-atom) in the same parent directory as [schema-gen](https://github.com/reaster/schema-gen), you can hard-code your configuration in the [KotlinGen](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/schema/kotlin/KotlinGen.groovy) constructor as follows:
```
KotlinGen()
{
    ...
    schemaURL = new URL('file:../kotlin-atom/src/main/resources/atom.xsd')
    srcDir = new File('../kotlin-atom/src/main/kotlin-gen')
} 
```
Then you can point your IDE at KotlinGen in the  [schema-gen](https://github.com/reaster/schema-gen) project and the generated code will be put in your kotlin-atom project.

#### Configuration
The code generator is designed to be highly customizable. In particular, how it names classes, properties, and enumerations is all encoded in configurable properties and lambdas. Here is a sampling of the properties that can be overridden in the `Gen` base class:
```
URL schemaURL = new URL('http://www.topografix.com/gpx/1/1/gpx.xsd')
File srcDir = new File('src/main/java-gen')
List<MVisitor> pipeline = []
PluralService pluralService = new PluralService()
def customPluralMappings = [:] //needed for irregular nouns: tooth->teeth, person->people
boolean useOptional = false //just effects Java code: Integer vs Optional<Integer>
String packageName = null
String addSuffixToEnumClass = 'Enum'
String removeSuffixFromType = 'Type'
String fileExtension = 'java'

Function<String,String> packageNameFunction = { ns -> packageName ?: ns ? GlobalFunctionsUtil.javaPackageFromNamespace(ns, true) : 'com.javagen.model' }
Function<String,String> enumNameFunction = { text -> GlobalFunctionsUtil.javaEnumName(text, false) }
Function<String,String> enumValueFunction = { text -> text }
Function<String,String> enumClassNameFunction = { text -> GlobalFunctionsUtil.enumClassName(text, addSuffixToEnumClass) }
Function<String,String> classNameFunction = { text -> GlobalFunctionsUtil.className(text, removeSuffixFromType) }
Function<String,String> propertyNameFunction = { text -> GlobalFunctionsUtil.legalJavaName(lowerCase(text)) }
Function<String,String> constantNameFunction = { text -> GlobalFunctionsUtil.javaConstName(text) }
Function<String,String> collectionNameFunction = { singular -> customPluralMappings[singular] ?: pluralService.toPlural(singular) }
Function<String,String> simpleXmlTypeToPropertyType
BiFunction<Gen,MClass,File> classOutputFileFunction = { gen,clazz -> new File(gen.srcDir, GlobalFunctionsUtil.pathFromPackage(clazz.fullName(),fileExtension))} //default works for Java

```
These properties can be set in the Gradle plugin for each of the target languages:
```
schemaGen {
    kotlin {
        srcDir = new File('../kotlin-gpx/src/main/kotlin')
    }
    swift{
        srcDir = new File('../swift-gpx/src/swift-gen')
    }
    java {
        srcDir = new File('src/main/java')
        packageName = 'com.javagen.model'
    }

 }

```

## Status
Although the design went through three iterations and is now stable as of 2017, the code is still being cleaned up. It has yet to be tested against a wide variety of schemas, so issues should be expected when trying new schemas. 

## Limitations
#### Xml Schema
##### Mixed Content
This code generator is not intended to support document-centric (versus data-centric) code like HTML. In particular, mixed content (mixed tags and text) is not well supported and will often be mapped to a single string property.
##### AttributeGroup
Currently, AttributeGroup are in-lined (i.e. expaned where they are referenced) and not mapped to a specific, re-usable entity (interface, class, trait, etc.)
##### Group
Currently, Group elements are in-lined (i.e. expanded where they are referenced) and not mapped to a specific, re-usable entity (interface, class, trait, etc.)
##### Union
Unions often consist of 2 or more `TextOnlyType`s merged together with their attached restrictions. If these restrictions are all enumerations, the union of unique values will be modeled properly. However, in other cases - say mixing numeric and string values - you will get a warning and probably an incorrect mapping.

___

## Architecture
For developers wishing to extend this framework, here is a quick overview of how it's put together.
#### XmlSchemaNormalizer
To simplify the schema-to-code translation process, the XML schema is normalized (references removed, etc.) as the first step in the code generation process. The results are stored in a Schema instance which tracks schema types (TestOnlyType, SimpleType and ComplextType), elements, attributes and other needed constructs. QNames support mixed namespace schemas.
#### Gen
The translation classes (JavaGen, KotlinGen, SwiftGen, etc.) walk the normalized schema and generate an abstract code model: MModule, MClass, MProperty, etc.
#### Callbacks
Code generation for supported third party libraries is handled via callback classes that typically set annotations and interfaces expected by the library. See [KotlinJacksonCallback](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/schema/kotlin/KotlinJacksonCallback.groovy) for an example. 
#### Emitters
An emitter exists for each supported language and takes the abstact model and converts it into the target computer langauge. See [JavaEmitter](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/schema/java/JavaEmitter.groovy) as an example.
#### PreEmitters
These do the grunt work of generating boiler-plate code for methods such as equals, hashCode and toString. The developer can simply add a method with the proper stereotype and the PreEmitter will generate the rest. See [SwiftPreEmitter](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/schema/swift/SwiftPreEmitter.groovy) as an example.
#### TypeRegistry
Each langauge needs a type registry, specific to it's supported types along with how to translate these from XML schema types.

___

## Support
The author has multiple projects in the pipeline, other passions plus bills to pay, but will try his best to support the community as time allows. In particular, if anybody would like to add language support (Python, C# or Go), test new schemas, contribute bug fixes, expand the documentation, or add test coverage, I'd be happy to support you. Finally, I'm available for contracting/consulting, especially if it involves expanding the framework to support your needs.
