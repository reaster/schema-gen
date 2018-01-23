/*
 * Copyright (c) 2018 Outsource Cafe, Inc.
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

package com.javagen.schema.xml

import spock.lang.Specification

class QNameSpec extends Specification {
    def "qname equals"() {
        given:
        def q1 = new QName(namespace: 'a', name:'A')
        def q2 = new QName(namespace: 'a', name:'A')
        expect:
        q1 == q2
    }
}