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

import com.javagen.schema.java.JavaGen
import com.javagen.schema.kotlin.KotlinGen
import com.javagen.schema.swift.SwiftGen
import groovy.transform.ToString
import org.gradle.api.Action

/**
 * Gradle extension that holds configuration and entry point for java, kotlin and swift code generators.
 *
 * @author Richard Easterling
 */
@groovy.transform.CompileStatic
@ToString(includePackage=false)
class GenExtension
{
    static final String NAME = 'schemaGen'

    JavaGen java
    KotlinGen kotlin
    SwiftGen swift

    void swift(Action<? super SwiftGen> action) {
        if (!swift)
            swift = new SwiftGen()
        action.execute(swift);
    }
    void kotlin(Action<? super KotlinGen> action) {
        if (!kotlin)
            kotlin = new KotlinGen()
        action.execute(kotlin);
    }
    void java(Action<? super JavaGen> action) {
        if (!java)
            java  = new JavaGen()
        action.execute(java);
    }
    boolean isNoAction() { java || kotlin || swift }
}
