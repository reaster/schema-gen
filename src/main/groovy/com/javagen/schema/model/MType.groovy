package com.javagen.schema.model


class MType extends MBase
{
	boolean primitive = false
	boolean builtIn = false
	String val // default value

	String toString() { name }
	
	static MType lookupType(String name) {
		MTypeRegistry.instance().lookupType(name)
	}
	static void registerType(String name, MType type) {
		MTypeRegistry.instance().registerType(name, type)
	}
	static void registerType(MClass type) {
		MTypeRegistry.instance().registerType(type.name, type)
	}

}
