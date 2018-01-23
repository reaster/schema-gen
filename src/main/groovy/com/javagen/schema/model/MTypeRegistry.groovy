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

import groovy.transform.CompileStatic

/**
 * Wraps language specific types in MType instances.
 *
 * usage: MTypeRegistry.instance().lookupType('String')
 */
@CompileStatic
abstract class MTypeRegistry
{
    Map<String,MType> types = [:]
    Map<String,MType> immutableTypes = [:]
    private static MTypeRegistry _instance

    MTypeRegistry(Map<String,MType> types)
    {
        this.types = types
        _instance = this
    }

    static boolean isInitialized()
    {
        return _instance != null
    }
    static void reset()
    {
        _instance = null
    }
    static MTypeRegistry instance()
    {
        if (!isInitialized())
            throw new IllegalStateException('MTypeRegistry has not been initialized. Create language-specific instance before calling.')
        _instance
    }

    /** return language-specific void type */
    abstract MType getVOID()
    /**
     * hook for handling language-specific situations where a simple map lookup won't work, like Java arrays.
     * should return null, if not special type.
     */
    abstract MType lookupTypeSpecial(String name)

    String lookupDefaultValue(String name, boolean mutable=true)
    {
        if (!name)
            return null
        MType t = (!mutable && immutableTypes) ? immutableTypes[name] : null
        if (!t)
            t = types[name]
        return (t && t.val!=null) ? t.val : 'null'
    }
    MType lookupType(String name)
    {
        MType result = types[name]
        if (!result)
            result = lookupTypeSpecial(name)
        result
    }
    void registerType(String name, MType type)
    {
        if (!type) {
            types.remove(name)
        } else {
            types[name] = type
        }
    }

    boolean containerRequiresPrimitiveWrapper(MCardinality cardinality)
    {
        false
    }
//    void registerType(MClass type)
//    {
//        registerType(type.name, new MType(name: type.name))
//    }

}
