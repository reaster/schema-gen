package com.javagen.schema.common

import com.javagen.schema.model.MClass
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MSource

import static com.javagen.schema.common.GlobalFunctionsUtil.lowerCase

import java.util.function.BiFunction
import java.util.function.Function

abstract class Gen
{
    List<CodeEmitter> pipeline = []
    File srcDir = new File('src/main/java-xml');
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
    BiFunction<Gen,MClass,File> classOutputFile = { gen,clazz -> new File(gen.srcDir, GlobalFunctionsUtil.pathFromPackage(clazz.fullName(),fileExtension))} //default works for Java

    /**
     * Build abstract code model from xml
     * @return abstract code model as a MModule root object
     */
    abstract MModule getModel()

    /**
     * entry point for code generator
     */
    def gen()
    {
        MModule rootModule = getModel()
        pipeline.each { visitor ->
            visitor.visit(rootModule)
        }
    }

    /**
     * Template support - return Gen instance properties as map for use as template binding
     */
    def templateBindingFromProperties(def instance = this)
    {
        def propsMap = instance.properties
        propsMap.remove('metaClass')
        propsMap.remove('class')
        propsMap
    }

    /** when assigned to classOutputFile, allows multiple code artifacts in one source files based on parent attr value */
    static File pathFromSourceFileName(Gen gen, MSource nested, String fileName, boolean exceptionOnFail=true)
    {
        if (!fileName && exceptionOnFail)
            throw new Error("no 'fileName' param provided for ${nested}")
        if (!fileName)
            fileName = 'Src'
        new File(gen.srcDir, "${fileName}.${gen.fileExtension}")
    }
    /** when assigned to classOutputFile, allows multiple code artifacts in one source files based on parent attr value */
    static File fileNmeFromAttr(Gen gen, MSource nested, String key='fileName', boolean exceptionOnFail=true)
    {
        String fileName = nested.nestedAttr(key)
        if (!fileName && exceptionOnFail)
            throw new Error("no '${key}' nested attribute found in ${nested} or it's parents")
        if (!fileName)
            fileName = 'NoFileNameAttrSet'
        new File(gen.srcDir, "${fileName}.${gen.fileExtension}")
    }
    /**
     * Generate code from template and bindings
     */
    def genTemplate(String templateText, File outputFile, Map binding)
    {
        def engine	= new groovy.text.SimpleTemplateEngine()
        def template = engine.createTemplate(templateText)
        def out = new FileOutputStream(outputFile, false)
        out << template.make(binding).toString();
        out.close()
    }
}
