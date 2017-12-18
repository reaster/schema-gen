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

package com.javagen.schema.model

/**
 * Used by MModule and MClass to allow nested types. Also manages two common source file emission scenarios:
 * <ul>
 * <li><file per class - Typically used by Java</li>
 * <li>file per module - Used by Kotlin and Swift</li>
 * </ul>
 *
 */
trait MSource
{
    File sourceFile
    def parent //MModule or MClass
    List<MClass> classes = []
    private Imports imports = new Imports(this)

    abstract String nestedAttr(String key)

    boolean isSource() { sourceFile != null }

    def getImports() { imports }

    def addClass(c) {
        if (c) {
            classes << c
            c.parent = this
        }
    }
    MClass lookupClass(String name) { classes.find { name == it.name } }

    /**
     * passes imports down to base classes
     */
    List<String> gatherSourceImports()
    {
        List<String> results = []
        if (sourceFile) {
            Set<String> set = [] as Set
            classes.each { MClass c ->
                c.imports.list.each {
                    set << it
                }
            }
            results = set.sort().collect()
        }
        results
    }
    /**
     * gathers imports from child classes and modules
     */
    static class Imports
    {
        Set<String> list = [] as Set
        def owner
        Imports(owner) { this.owner=owner }
        def leftShift(item) {
            if ((owner.parent instanceof MModule)) {
                list << item
            } else {
                owner.parent.imports << item
            }
        }
        boolean isEmpty() { list.isEmpty() }
        def each(Closure c) { list.each(c) }
    }
}