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

    abstract MType getVOID()

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
        types[name]
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
