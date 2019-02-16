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

package com.javagen.schema.dart

import org.junit.Test

import static com.javagen.schema.dart.DartUtil.*
import static junit.framework.TestCase.*

class DartUtilTest
{

    @Test
    void testToGeneratedSource()
    {
        File f = new File('myproject/lib/my_code.dart')
        //toGeneratedSourceFileName(File src, String suffix='.g', String subfolder=null)
        assertTrue('default params', toGeneratedSourceFileName(f).toString().endsWith('myproject/lib/my_code.g.dart'))
        assertTrue(toGeneratedSourceFileName(f,'-gen','src').toString().endsWith('myproject/lib/src/my_code-gen.dart'))
    }

    @Test
    void testEscapeDartString()
    {
        assertEquals('default params', '\\$', escapeDartString('$'))
        assertEquals('default params', '\\$\\$', escapeDartString('$$'))
    }
    @Test
    void testDartEnumName()
    {
        assertEquals('id',  dartEnumName('ID', false, false))
        assertEquals('ID',  dartEnumName('id', true, false))
        assertEquals('ID',  dartEnumName('ID', false, true))
        assertEquals('id',  dartEnumName('id', false, true))
    }


}
