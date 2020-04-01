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

package com.javagen.schema.common

import com.javagen.schema.model.MClass
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MSource
import groovy.text.SimpleTemplateEngine

import static com.javagen.schema.common.GlobalFunctionsUtil.lowerCase

import java.util.function.BiFunction
import java.util.function.Function

/**
 * Base class for code generators including the gen() entry point method. Subclasses will return an abstract code model
 * contained in a root MModule instance, then apply a code emitter to the model to produce source code for the target
 * language.
 * <p>In the case of XML schema to Java code generation, the flow is:
 * <pre>
 *     XML Schema -> XmlSchemaNormalizer -> JavaGen -> JavaPreEmitter -> JavaEmitter -> Java source code
 * </pre>
 * The pipeline is called once the abstract model is completed and can include any number of CodeEmitters.
 *
 * <p>Most of the configuration for the code generator is in this class and consists mostly of configurable code naming
 * functions and conventions. It also includes the input (schemaURL) and output (srcDir) locations.
 *
 * <p>This class is designed to be schema-type agnostic, meaning the model can come from non-XML schema sources such as
 * JSON schemas or database schemas.
 *
 * TODO Naming functions should take a second namespace parameter to optionally allow distinct names based on namespaces.
 * TODO Probably should use closures for naming functions to make the code more Groovy.
 *
 * @author Richard Easterling
 */
abstract class Gen
{
    URL schemaURL = new URL('http://www.topografix.com/gpx/1/1/gpx.xsd')
    File srcDir = null //new File('src/main/java-gen');

    List<CodeEmitter> pipeline = []

    Map<String,String> typeOverrideMap = [:]
    PluralService pluralService = new PluralService()
    def customPluralMappings = [:] //needed for irregular nouns: tooth->teeth, person->people
    boolean useOptional = false //just effects Java code: Integer vs Optional<Integer>
    boolean printSchema = false
    File projectDir = null
    String projectName = null
    String packageName = null
    String srcFolder = 'src/main/java-gen'
    String addSuffixToEnumClass = 'Enum'
    String removeSuffixFromType = 'Type'
    String fileExtension = 'java'
    String defaultEnumValue //ignored if null, otherwise forces all enum classes to don't define defaults to use this value
    boolean sortEnumValues = true
    boolean includeIfNull = false
    boolean includeJsonSupport = true
    boolean emitInnerClasses = false //emit nested Kotlin classes as inner classes

    Function<String,String> packageNameFunction = { ns -> packageName ?: ns ? GlobalFunctionsUtil.javaPackageFromNamespace(ns, true) : 'com.javagen.model' }
    Function<String,String> enumNameFunction = { text -> GlobalFunctionsUtil.javaEnumName(text, false) }
    Function<String,String> enumValueFunction = { text -> text }
    Function<String,String> enumClassNameFunction = { text -> GlobalFunctionsUtil.enumClassName(text, addSuffixToEnumClass) }
    Function<String,String> classNameFunction = { text -> GlobalFunctionsUtil.className(text, removeSuffixFromType) }
    Function<String,String> propertyNameFunction = { text -> GlobalFunctionsUtil.javaVariableName(text) }
    Function<String,String> constantNameFunction = { text -> GlobalFunctionsUtil.javaConstName(text) }
    Function<String,String> collectionNameFunction = { singular -> customPluralMappings[singular] ?: pluralService.toPlural(singular) }
    Function<String,String> simpleXmlTypeToPropertyType
    BiFunction<Gen,MClass,File> classOutputFileFunction = { gen, clazz ->
        File srcDir = gen.srcDir
        new File(srcDir, GlobalFunctionsUtil.pathFromPackage(clazz.fullName(), fileExtension))
    } //default works for Java

    File getSrcDir()
    {
        if (!srcDir) {
            if (projectDir) {
                if (!projectName)
                    projectName = projectDir.name
            } else {
                if (!projectName)
                    projectName = 'javagen'
                projectDir = new File(projectName)
            }
            if (!srcFolder)
                throw new IllegalStateException('srcFolder not set')
            srcDir = new File(projectDir, srcFolder)
        }
        srcDir
    }

    String projectPath(File srcFile, boolean includeProjectFolder=true)
    {
        def segments = Arrays.asList(srcFile.absolutePath.split('/')).reverse()
        String result = ''
        String stop = projectName ? projectName : srcFolder.split('/')[0]
        for(def segment : segments) {
            if (stop == segment) {
                return includeProjectFolder ? ("${projectName ? projectName : stop}/${result}") : result
            }
            result = result ? "${segment}/${result}" : segment
        }
        result
    }

    /**
     * Build abstract code model from (XML) schema.
     *
     * @return abstract code model as a MModule root object
     */
    abstract MModule getModel()

    /**
     * entry point for code generator
     */
    def gen()
    {
        if (!customPluralMappings.isEmpty())
            pluralService = new PluralService(customPluralMappings) //pickup custom map
        MModule rootModule = getModel()
        pipeline.each { visitor ->
            visitor.visit(rootModule)
        }
    }

    /**
     * Template support - currently not being used.
     * @return Gen instance properties as map for use as template binding
     */
    def templateBindingFromProperties(def instance = this)
    {
        def propsMap = instance.properties
        propsMap.remove('metaClass')
        propsMap.remove('class')
        propsMap
    }

    /**
     * Generate code from template and bindings - currently not being used.
     */
    def static genTemplate(String templateText, File outputFile, Map binding)
    {
        def engine	= new SimpleTemplateEngine()
        def template = engine.createTemplate(templateText)
        def out = new FileOutputStream(outputFile, false)
        out << template.make(binding).toString()
        out.close()
    }

    /** Meant to be assigned to classOutputFileFunction to generate a source file name. */
    static File pathFromSourceFileName(Gen gen, MSource nested, String fileName, boolean exceptionOnFail=true)
    {
        if (!fileName && exceptionOnFail)
            throw new Error("no 'fileName' param provided for ${nested}")
        if (!fileName)
            fileName = 'Src'
        if (nested && nested instanceof MModule && ((MModule)nested).name) {
            String packageName = ((MModule)nested).fullName().replace('.', '/')
            return new File(new File(gen.srcDir, packageName), "${fileName}.${gen.fileExtension}")
        } else {
            new File(gen.srcDir, "${fileName}.${gen.fileExtension}")
        }
    }

//    /** when assigned to classOutputFileFunction, allows multiple code artifacts in one source files based on parent attr value */
//    static File fileNmeFromAttr(Gen gen, MSource nested, String key='fileName', boolean exceptionOnFail=true)
//    {
//        String fileName = nested.nestedAttr(key)
//        if (!fileName && exceptionOnFail)
//            throw new Error("no '${key}' nested attribute found in ${nested} or it's parents")
//        if (!fileName)
//            fileName = 'NoFileNameAttrSet'
//        new File(gen.srcDir, "${fileName}.${gen.fileExtension}")
//    }
}
