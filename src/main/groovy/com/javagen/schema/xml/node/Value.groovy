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
