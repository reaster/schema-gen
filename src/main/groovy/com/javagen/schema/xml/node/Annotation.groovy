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

package com.javagen.schema.xml.node

class Annotation extends Node
{
    java.util.List<Appinfo> appinfo = []
    java.util.List<Documentation> documentation = []
    @Override java.util.List<String> appinfoValues(String key) {
        appinfo.findAll{ it.text.startsWith(key) }.collect { it.appinfoValue(key) } ?: []
    }
    @Override java.util.List<String> docLines() {
        java.util.List<String> lines = []
        documentation.each { doc ->
            doc.text.split('\n').each {
                lines << it.trim()
            }
        }
        lines
    }
}
