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

package com.javagen.schema.xml.node

import groovy.transform.ToString
import com.javagen.schema.xml.node.Value.NS
import com.javagen.schema.xml.node.Value.ProcessContents
import com.javagen.schema.xml.node.Value.Namespace

@ToString(includeSuper=true,includePackage=false)
class Any extends Element
{
    ProcessContents processContents = ProcessContents.strict
    NS namespace = Namespace.ANY
    @Override void setType(TextOnlyType type)
    {
        super.setType(type)
    }
}
