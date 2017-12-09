package com.javagen.gen.model

class MClass extends MType implements MSource
{
	def parent //module or class
	private boolean _abstract
	private boolean _static
	private boolean _interface
	private boolean extension
	private boolean data
	boolean ignore = false //do not emmit class
	protected def scope = 'public'
	private boolean struct
	Map<String,MField> fields = [:]
	List<MMethod> methods = []
	def _implements = [] as Set<String>

	def MClass() {
		scope = 'public'
		_abstract = false
	}
	@Override
	String fullName() {
		parent ? parent.fullName()+'.'+name : name
	}
	def addField(f) {
		fields[f.name] = f
		f.parent = this
	}
	def addMethod(m) {
		methods << m
		m.parent = this
	}
	boolean hasMethod(String name)
	{
		methods.find{ m -> name == m.name} != null
	}
	MMethod findMethod(MMethod.Stereotype stereotype)
	{
		methods.find{ m -> stereotype == m.stereotype}
	}
	MModule parentModule()
	{
		def p = parent
		while (p && !(p instanceof MModule)) {
			p = p.parent
		}
		return p
	}
	@Override String nestedAttr(String key)
	{
		attr[key] ?: parent?.nestedAttr(key)
	}
	MClass setExtension(boolean e) { extension = e; return this  }
	boolean isExtension() { extension }
	MClass setAbstract(boolean a) { _abstract = a; return this  }
	boolean isAbstract() { _abstract }
	MClass setStatic(boolean s) { _static = s; return this  }
	boolean isStatic() { _static }
	MClass setInterface(i) { _interface = i; return this  }
	boolean isInterface() { _interface }
	boolean isStruct() { struct }
	MClass setStruct(boolean s) { struct = s; return this  }
	def lookupField(String name) { fields[name] }
	def fieldsWithAttr(String attrName) { fields.values().findAll { it.attr[attrName] } }
	def fieldsWithoutAttr(String attrName) { fields.values().findAll { !it.attr[attrName] } }
	Set<String> getImplements() { _implements }
	def setImplements(Set<String> _implements) { this._implements = _implements }
	boolean isEnum() { false }
}
