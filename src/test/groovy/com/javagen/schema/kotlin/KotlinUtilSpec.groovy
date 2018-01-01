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

package com.javagen.schema.kotlin

import static com.javagen.schema.kotlin.KotlinUtil.*
import spock.lang.Specification

class KotlinUtilSpec extends Specification
{
    def testLegalKotlinClassName()
    {
        expect: "a set of candidate class names is converted to legal, non-conflicting names"
        legalKotlinClassName('TheDudeType', 'Type') == 'TheDude'
        legalKotlinClassName('TheDudeType', null) == 'TheDudeType'
        legalKotlinClassName('Type') == 'Type'
        legalKotlinClassName('Set') == 'Set_'
        legalKotlinClassName('array') == 'Array_'
        legalKotlinClassName('string') == 'String_'
        legalKotlinClassName('Any') == 'Any_'
        legalKotlinClassName('StringBuilder') == 'StringBuilder_'
        legalKotlinClassName('0Dude') == '_0Dude'
    }

    def testCamelBackKotlinClass()
    {
        expect: "a set of candidate class names is converted to legal, non-conflicting names"
        camelBackKotlinClass('type') == 'Type'
        camelBackKotlinClass('set') == 'Set_'
        camelBackKotlinClass('my_house') == 'MyHouse'
        camelBackKotlinClass('my---purple-_eye') == 'MyPurpleEye'
        camelBackKotlinClass('any') == 'Any_'
        camelBackKotlinClass('StringBuilder') == 'Stringbuilder'
    }
}
