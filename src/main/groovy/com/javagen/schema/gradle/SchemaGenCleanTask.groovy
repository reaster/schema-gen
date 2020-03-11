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

import com.javagen.schema.common.Gen
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Gradle task to delete generated source directories.
 *
 * @author Richard Easterling
 */
class SchemaGenCleanTask extends DefaultTask
{
    static final String NAME = 'genClean'
    private static final Logger LOG = LoggerFactory.getLogger(SchemaGenCleanTask)

    SchemaGenCleanTask()
    {
        group = 'Schema Gen'
        description = 'deletes XML schema code generation target directories'
    }


    @TaskAction
    def gen()
    {
        GenExtension extension = project.schemaGen

        if (!extension) {
            LOG.info('no \'schema\' extension defined, skipping {} task', NAME)
            return
        }
        if (extension.java) {
            LOG.info('deleting schemaGen.java.srcDir={}', extension.java.srcDir)
            deleteSanityCheck(project.projectDir, extension.java)
            deleteSanityCheck(new File(project.projectDir, 'src/main/java'), extension.java)
            deleteSanityCheck(project.rootProject.projectDir, extension.java)
            project.delete(outputDir(extension.java))
        }
        if (extension.kotlin) {
            LOG.info('deleting schemaGen.kotlin.srcDir={}', extension.kotlin.srcDir)
            deleteSanityCheck(project.projectDir, extension.kotlin)
            deleteSanityCheck(new File(project.projectDir, 'src/main/kotlin'), extension.kotlin)
            deleteSanityCheck(project.rootProject.projectDir, extension.kotlin)
            project.delete(outputDir(extension.kotlin))
        }
        if (extension.swift) {
            LOG.info('deleting schemaGen.swift.srcDir={}', extension.swift.srcDir)
            deleteSanityCheck(new File(project.projectDir, 'src'), extension.swift)
            deleteSanityCheck(project.projectDir, extension.swift)
            deleteSanityCheck(project.rootProject.projectDir, extension.swift)
            project.delete(outputDir(extension.swift))
        }
        if (extension.dart) {
            LOG.info('deleting schemaGen.dart.srcDir={}', extension.dart.srcDir)
            deleteSanityCheck(new File(project.projectDir, 'src'), extension.dart)
            deleteSanityCheck(project.projectDir, extension.dart)
            deleteSanityCheck(project.rootProject.projectDir, extension.dart)
            project.delete(outputDir(extension.dart))
        }
    }

    private File outputDir(Gen gen)
    {
        return project.file(gen.srcDir)
    }

    private deleteSanityCheck(File target, Gen gen)
    {
        if (outputDir(gen) == target) {
            throw new GradleException("For your own protection, you can't delete the project, root project or main source directory: ${target}")
        }
    }

}
