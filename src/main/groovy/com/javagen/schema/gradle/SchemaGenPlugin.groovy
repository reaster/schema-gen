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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Simple Gradle plugin to generate object model and marshalling code from XML schema. Supports java, kotlin and swift.
 *
 * @author Richard Easterling
 */
@groovy.transform.CompileStatic
class SchemaGenPlugin implements Plugin<Project>
{
    private static final Logger LOG = LoggerFactory.getLogger(SchemaGenPlugin)

    @Override void apply(Project project)
    {
        if (project.plugins.hasPlugin(SchemaGenPlugin)) {
            return
        }
        LOG.info("Applying SchemaGenPlugin plugin")

        project.extensions.create(GenExtension.NAME, GenExtension)
        project.task(type: SchemaGenTask, SchemaGenTask.NAME)
        project.task(type: SchemaGenCleanTask, SchemaGenCleanTask.NAME)
    }

}
