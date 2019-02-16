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

import spock.lang.Specification

class AnnotationTest extends Specification {
    def "AppinfoValue"() {
        when:
        def anno = new Annotation(appinfo: [new Appinfo(text:'fee:fi=fo'),new Appinfo(text:'fee:fi=fum'),new Appinfo(text:'frick:a=frack'),])
        then:
        ['frack'] == anno.appinfoValues('frick:a')
        ['fo','fum'] == anno.appinfoValues('fee:fi')
    }

    def "DocLines"() {
        when:
        def anno = new Annotation(documentation: [new Documentation(text: "\ta\n  b  \n\tc\t")])
        then:
        ['a','b','c'] == anno.docLines()
        when:
        def anno2 = new Annotation(documentation: [new Documentation(text: "\ta\n  b  \n\tc\t"),new Documentation(text: "\n  d  ")])
        then:
        ['a','b','c','','d'] == anno2.docLines()
    }
}
