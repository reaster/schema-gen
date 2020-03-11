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

package com.javagen.schema.gradle

import com.javagen.schema.common.GlobalFunctionsUtil
import com.javagen.schema.dart.DartTypeRegistry
import com.javagen.schema.java.JavaTypeRegistry
import com.javagen.schema.kotlin.KotlinTypeRegistry
import com.javagen.schema.swift.SwiftTypeRegistry
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import com.javagen.schema.common.Gen

/**
 * Gradle task to generate object model and marshalling code from XML schema. Supports java, kotlin and swift.
 *
 * @author Richard Easterling
 */
class SchemaGenTask extends DefaultTask
{
    static final String NAME = 'gen'

    SchemaGenTask()
    {
        group = 'Schema Gen'
        description = 'generates object model and marshalling code from XML schema. Supports java, kotlin and swift.'
    }

    @TaskAction
    def gen()
    {
        GenExtension extension = project.schemaGen

        if (!extension) {
            extension = new GenExtension()
            //println "no 'schema' extension defined, creating default JavaGen config"
        }
        if (extension.java) {
            new JavaTypeRegistry()
            adjustProjectFilePaths(extension.java)
            println "schemaGen.java.srcDir=${extension.java.srcDir}, schemaURL=${extension.java.schemaURL}"
            extension.java.gen()
        }
        if (extension.kotlin) {
            new KotlinTypeRegistry()
            adjustProjectFilePaths(extension.kotlin)
            println "schemaGen.kotlin.srcDir=${extension.kotlin.srcDir}, schemaURL=${extension.kotlin.schemaURL}"
            extension.kotlin.gen()
        }
        if (extension.swift) {
            new SwiftTypeRegistry()
            adjustProjectFilePaths(extension.swift)
            println "schemaGen.swift.srcDir=${extension.swift.srcDir}, schemaURL=${extension.swift.schemaURL}"
            extension.swift.gen()
        }
        if (extension.dart) {
            new DartTypeRegistry()
            adjustProjectFilePaths(extension.dart)
            println "schemaGen.dart.srcDir=${extension.dart.srcDir}, schemaURL=${extension.dart.schemaURL}"
            extension.dart.gen()
        }
        if (extension.noAction) {
            println "SchemaGenPlugin is applied but no schema DSL found in gradle.build file, add 'schemaGen{ java{} kotlin{} dart{} swift{} }'"
        }
    }
    private void adjustProjectFilePaths(Gen gen)
    {
        gen.srcDir = project.file(gen.srcDir)
        final String relativePath = GlobalFunctionsUtil.containsRelativeFilePath(gen.schemaURL)
        if (relativePath) {
            gen.schemaURL = project.file(relativePath).toURI().toURL()
        }
    }

}
