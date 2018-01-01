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

package com.javagen.schema.xml.node

/**
 * A compositor where the elements in the group must appear in the specified order.
 *
 * Parents: group, choice, sequence, complexType, restriction (simpleContent), extension (simpleContent), restriction (complexContent), extension (complexContent)
 * Content: (annotation?, (element | group | choice | sequence | any)*)
 *
 * @author Richard Easterling
 */
class Sequence extends Compositor {
}
