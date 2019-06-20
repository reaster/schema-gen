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

package com.javagen.schema.dart


import com.javagen.schema.common.PluralService
import com.javagen.schema.java.JavaGen
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.model.MTypeRegistry
import com.javagen.schema.xml.XmlSchemaNormalizer

import static com.javagen.schema.common.GlobalFunctionsUtil.lowerCase
import static com.javagen.schema.common.GlobalFunctionsUtil.upperCase

/**
 * Translate XML schema to Dart 2.1 code. The model is generated by JavaGen with select methods and properties
 * set to adjust for Dart-specific requirements. Dart AST visitors are configured in the pipeline to generate toJson/fromJson
 * methods (DartJsonEmitter) and put enum types in a separate source file (DartSupportEmitter).
 *
 * <p>A XmlNodeCallback can be used to apply specific third-party library annotations to the object model, allowing one
 * to easily switch technologies. For example one could swap the DartToJsonCallback with a DartJaxbCallback without
 * having to rewrite the DartGen object model translation code.
 *
 * This class is the entry point for Dart code generation.
 *
 * @author Richard Easterling
 */
class DartGen extends JavaGen
{
    String sourceFileName = null

    //java-specific methods not needed in Dart:
    @Override void optionalToPrimitiveWrapper(MProperty property) {}
    @Override void mapPropertyAccessors(MProperty property) {}
    @Override void addEnumValueSupport(MEnum enumClass) {}

    /** remove srcFolder to conform to import syntax. */
    @Override String projectPath(File srcFile, boolean includeProjectFolder=true)
    {
        def path = super.projectPath(srcFile, includeProjectFolder)
        srcFolder ? path.replace(srcFolder+'/', '') : path
    }


//    @Override String enumClassName(String tag)
//    {
//        int i = nestedStack.size()
//        def clazz = i == 0 ? null : nestedStack.get(i-1)
//        String name = enumClassNameFunction.apply(tag)
//        while(clazz instanceof MClass) {
//            name = "${clazz.name}_${name}"
//            i--
//            clazz = i == 0 ? null : nestedStack.get(i-1)
//        }
//        name
//    }


    DartGen()
    {
        super(true)
        packageName = 'model'
        srcFolder = 'lib'
        fileExtension = 'dart'
        useOptional = true //only applies to Java
        anyType = 'anyType'
        propertyScope = 'public'
        choiceCollectionWrapperConstructor = false
        simpleXmlTypeToPropertyType = { xmlType ->
            DartTypeRegistry.simpleXmlTypeToPropertyType[xmlType]
        }
        if ( ! MTypeRegistry.isInitialized() )
            new DartTypeRegistry()
        callback = new DartToJsonCallback(this, false)
        pipeline = [
                new DartPreEmitter(gen: this),
                new DartJsonEmitter(gen: this),
                new DartSupportEmitter(gen: this),
                new DartMixinEmitter(gen: this),
                new DartEmitter(gen: this)
        ]
        enumNameFunction = { text -> DartUtil.dartEnumName(text, false) }
        enumValueFunction = { text -> DartUtil.dartEnumValue(text, false) }
        propertyNameFunction = { text -> DartUtil.legalDartName(lowerCase(text)) }
        constantNameFunction = { text -> DartUtil.dartConstName(text) }
        classNameFunction = { text -> DartUtil.legalDartClassName(text) }
        packageNameFunction = { ns -> packageName }
    }

    @Override def gen()
    {
        if (!customPluralMappings.isEmpty())
            pluralService = new PluralService(customPluralMappings) //pickup custom map
        schema = new XmlSchemaNormalizer().buildSchema(schemaURL)
        visit(schema)
        MModule rootModule = getModel()
        if (!sourceFileName) //if no source file name defined, use first root element name
            sourceFileName = schema.rootElements.isEmpty() ? 'javagen' : upperCase(schema.rootElements.first().name)
        sourceFileName = sourceFileName.toLowerCase()
        rootModule.sourceFile = pathFromSourceFileName(this, rootModule, sourceFileName)
        pipeline.each { visitor ->
            visitor.visit(rootModule)
        }
    }
}
