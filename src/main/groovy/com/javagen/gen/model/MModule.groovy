package com.javagen.gen.model

class MModule extends MBase implements MSource
{
	Map<String,MModule> children = [:]

	MModule() {
		name = ''
	}
	@Override
	String fullName() {
		parent ? parent.fullName()+'.'+name : name
	}
	def child(MModule m) {
		children[m.name] = m
	}
	String nestedAttr(String key)
	{
		attr[key] ?: parent?.nestedAttr(key)
	}

}
