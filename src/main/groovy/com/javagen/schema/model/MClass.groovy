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

class MClass extends MType implements MSource
{
	def parent //module or class
	private boolean _abstract
	private boolean _static
	private boolean _interface
	private boolean extension //Swift and Kotlin
	private boolean mixin = false //Dart
	private boolean data
	/** do not emmit class */
	boolean ignore = false
	protected def scope = 'public'
	private boolean struct
	Map<String,MField> fields = [:]
	List<MMethod> methods = []
	List<String> _implements = []
	String _extends = null

	MClass() {
		scope = 'public'
		_abstract = false
	}

	Map<String,MField> inheritedFields(boolean includeThis = true) {
		Map<String,MField> result = new LinkedHashMap<>()
		MClass c = includeThis ? this : (this.extends ? (MClass)lookupType(this.extends) : null)
		while (c != null) {
			c.fields.each { it -> result[it.key] = it.value }
			c = c.extends ? lookupType(c.extends) : null
		}
		result
	}
	MField firstField() {
		inheritedFields().values().first()
	}

	@Override
	String fullName() {
		parent ? parent.fullName()+'.'+name : name
	}
	def addField(MField f) {
		fields[f.name] = f
		f.parent = this
	}
	def addMethod(MMethod m) {
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
	@Override boolean isTypeOf(MType t) {
		if (t instanceof MClass) {
			MClass c = this
			while(c) {
				c = c.extends ? (MClass)lookupType(c.extends) : null
				if (c == t) {
					return true
				}
			}
		}
		false
	}

	/** single property class as a LIST, SET or ARRAY */
	@Override boolean isListWrapper() {
		Map<String,MField> allFields = inheritedFields()
		allFields.size() == 1 && allFields.values().first().cardinality in [MCardinality.ARRAY, MCardinality.LIST, MCardinality.SET]
	}
	/** single property class ussualy as a MAP or LINKEDMAP */
	@Override boolean isMapWrapper()
	{
		Map<String,MField> allFields = inheritedFields()
		allFields.size() == 1 && allFields.values().first().cardinality in [MCardinality.MAP, MCardinality.LINKEDMAP]
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
	MClass setExtends(String extendsSuperClass) { _extends= extendsSuperClass; return this  }
	String getExtends() { _extends }
	boolean hasSuper() { _extends != null }

	MClass setStruct(boolean s) { struct = s; return this  }
	def lookupField(String name) { fields[name] }
	def fieldsWithAttr(String attrName) { fields.values().findAll { it.attr[attrName] } }
	def fieldsWithoutAttr(String attrName) { fields.values().findAll { !it.attr[attrName] } }
	List<String> getImplements() { _implements }
	def setImplements(List<String> _implements) { this._implements = _implements }
	boolean isEnum() { false }
}
