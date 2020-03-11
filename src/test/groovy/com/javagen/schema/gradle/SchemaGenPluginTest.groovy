/*
 * Copyright (c) 2020 Outsource Cafe, Inc.
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

import org.gradle.api.Project
import org.junit.Test
import org.gradle.testfixtures.ProjectBuilder

import static junit.framework.TestCase.assertTrue

class SchemaGenPluginTest {
    @Test
    public void schemaGenPluginAddsGenTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'com.javagen.schema-gen'
        def gen = project.tasks.gen
        println(gen)
        assertTrue( (gen instanceof SchemaGenTask) )
    }
    @Test
    public void schemaGenPluginAddsGenCleanTaskToProject() {
        Project project = ProjectBuilder.builder().build()
        project.pluginManager.apply 'com.javagen.schema-gen'
        def genClean = project.tasks.genClean
        println(genClean)
        assertTrue( (genClean instanceof SchemaGenCleanTask) )
    }
}
