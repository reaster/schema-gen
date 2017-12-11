# schema-gen
schema-gen is a multi-language, interoperable, XML Schema code generator. The generated code is well suited for reading, writing and minipulating data defined by a XML schema. In particular, it was designed to reduce the amount of coding needed to write cross-language, mobile, client-server applications. Currently supported languages are Java, Kotlin and Swift 4. 
Written in Groovy, it's packaged as a Gradle plugin.
##Status
Although the design went through three iterations and is now stable, as of 2017, the code is still being cleaned up. The Gradle plugin works, but is half-baked. Lastly, it has yet to be tried against a wide variety of schemas and due to the complexity of the task, issues should be expected. 
## Currently Supported Languages

###Java
**Features:** Generated POJOs contain [Jackson](https://github.com/FasterXML/jackson-dataformat-xml) annotations supporting reading and writing both JSON and XML documents. XML Schema restrictions are also translated into [Bean Validation 2.0](http://beanvalidation.org/) annotations. Equals, hashCode and toString methods are also generated to facilitate testing. See the [java-gpx](https://github.com/reaster/schema-gen-examples/tree/master/java-gpx) sample project.

**Usage:** The main entry point is [com.javagen.gen.java.JavaGen](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/gen/java/JavaGen.groovy) which can be invoked directly or via the gradle plugin. By default, the generated code is placed in the src/main/java-gen folder to keep it separate from hand-written code.

**Limitations:** Java is not the best language for extending generated code with business logic in a maintainable manner. If you have to regenerate, you may have to manually merge the hand-written and generated code.

###Kotlin
**Features:** By leveraging [data classes](https://kotlinlang.org/docs/reference/data-classes.html), Kotlin generated code is very concise. Classes contain [Jackson](https://github.com/FasterXML/jackson-dataformat-xml) annotations supporting reading and writing both JSON and XML documents. XML Schema restrictions are also translated into [Bean Validation 2.0](http://beanvalidation.org/) annotations. Extending generated code with business logic can be acheived using [extensions](https://kotlinlang.org/docs/reference/extensions.html). See the [kotlin-gpx](https://github.com/reaster/schema-gen-examples/tree/master/kotlin-gpx) sample project.

**Usage:** The main entry point is [com.javagen.gen.kotlin.KotlinGen](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/gen/kotlin/KotlinGen.groovy) which can be invoked directly or via the gradle plugin. By default, the generated code is placed in the src/main/kotlin-gen folder to keep it separate from hand-written code.

**Limitations:** The code generator attemps to create no-argument constructors by setting default values on every property, which in some cases can cause problems. To minimize unessasary annotations, the generated code requires Java 8 parameter name support. See the build.gradle and unit tests in [kotlin-gpx](https://github.com/reaster/schema-gen-examples/tree/master/kotlin-gpx) proper setup and configuration.

###Swift
**Features:** schema-gen generates code utilizing the built-in Encodable and Decodable JSON support introduced in Swift 4. Extending generated code with business logic can be acheived using Extensions. See the [swift-gpx](https://github.com/reaster/schema-gen-examples/tree/master/swift-gpx) sample project.

**Usage:** The main entry point is [com.javagen.gen.swift.SwiftGen](https://github.com/reaster/schema-gen/blob/master/src/main/groovy/com/javagen/gen/swift/SwiftGen.groovy) which can be invoked directly or via the gradle plugin. By default, the generated code is placed in the src/main/swift-gen folder to keep it separate from hand-written code.

**Limitations:** Swift only supports JSON serialization. Given a good XMLEncoder, XML support should easy to add. (I wrote [saxy](https://github.com/reaster/saxy) in Objective-C last time around and it's somebody else's turn to do this for Swift ;-)

##Usage
TODO - Gradle plugin

##Limitations
####Xml Schema
Not intended to support document-centric code like HTML. In particular, mixed content (mixed tags and text) is not well supported.

##Architecture
For developers wishing to extend this framework, here is a quick overview of how it's put together.
####SchemaNormalizer
The simplify the translation process, the XML schema is normalized (references removed, etc.) by this class as the first stage in the code generation pipeline. The results are stored in a Schema instance which tracks schema types (TestOnlyType, SimpleType and ComplextType), elements, attributes and other needed constructs. QNames support mixed namespace schemas.
####Gen
The translation classes (JavaGen, SwiftGen, etc.) walk the normalized schema and generate an abstract code model (MModule, MClass, MProperty, etc.)
####Callbacks
Code generation for supported third party libraries are done via callback classes that typicaly set annotations and interfaces to support the data encoding process (see KotlinJacksonCallback for an example). 
####Emitters
An emitter exists for each supported language and takes the abstact model and converts it into the target computer langauge.
####PreEmitters
These do the grunt work of generating boiler-plate code for methods such as equals, hashCode and toString. The developer can simply add a method with the proper stereotype and the pregenerator will generate the rest. 
####TypeRegistry
Each langauge needs a type registry, specific to it's supported types along with how to translate these from XML schema types.

##Support
I have multiple projects in the pipeline and need to move on. If anybody would like to add language support, test new schemas, contribute bug fixes or fill in the test coverage I'd be happy to support you. Lastly, I'm available for contracting/consulting work if that meets your needs.
