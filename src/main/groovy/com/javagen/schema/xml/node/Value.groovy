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

@ToString(includeSuper=true,includePackage=false,ignoreNulls=true)
abstract class Value extends Node
{
    enum ProcessContents{lax, skip, strict}
    interface NS { String toString() }
    enum Namespace implements NS {
        ANY('##any'),
        OTHER('##other'),
        TARGET_NAMESPACE('##targetNamespace'),
        LOCAL('##local');
        final String ns
        private Namespace(String ns) { this.ns=ns }
        String toString() { ns }
        private static Map<String,NS> cache = values().collectEntries { [it.toString(),it] }
        private static class _NS implements NS { String ns; _NS(String ns) { this.ns=ns }; String toString() { ns } }
        static NS lookup(String ns) {
            if (!ns) return null
            NS _ns = cache.get(ns);
            if (!_ns) { _ns = new _NS(ns); cache.put(ns, _ns); }
            _ns
        }
    }
    String _default
    String fixed
    TextOnlyType type
    void setDefault(String _default) { this._default = _default }
    String getDefault() { _default }
}
