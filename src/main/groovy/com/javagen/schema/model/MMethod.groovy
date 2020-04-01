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

import com.javagen.schema.common.CodeEmitter

class MMethod extends MBase
{
	enum Stereotype {unknown, constructor, canonicalConstructor, getter, setter, adder, putter, toString, toStringBuilder, equals, hash, toJson, fromJson, equalsList, equalsMap, equalsSet}
	enum IncludeProperties {noProperties, finalProperties, allProperties}
	Stereotype stereotype = Stereotype.unknown
	IncludeProperties includeProperties
	def parent //MClass or MModule
	def refs = [:]
	private MBind type
	List<MBind> params = []
	protected def scope = 'public'
	protected boolean _abstract
	protected boolean _static
	protected boolean _final
	boolean singleExpr = false
	boolean override = false
	boolean factory = false
	boolean operator = false //operator overload in Dart and Kotlin
	boolean getter = false //just emits 'set' Dart modifier
	boolean setter = false //just emits 'get' Dart modifier
	/** include default value in param declarations */
	boolean includeDefaultValue = false
	/** signature takes two params: lambda(MMethod m, MVisitor v) and writes text to v.out */
	Closure body = null

	MMethod() { }
	MMethod(String name, MField field, Stereotype stereotype) {
		this.name = name
		refs['property'] = field
		this.stereotype = stereotype
	}
	boolean isConstructor() { stereotype == Stereotype.constructor || stereotype== Stereotype.canonicalConstructor }
	MMethod setAbstract(a) { _abstract = a; return this }
	boolean isAbstract() { _abstract }
	MMethod setStatic(s) { _static = s; return this }
	boolean isStatic() { _static }
	MMethod setFinal(f) { _final = f; return this }
	boolean isFinal() { _final }
	void setType(MBind type) { this.type = type }
	void setType(MType type) { this.type = new MBind(type: type) }
	void setType(String typeName) {
		MType type = MType.lookupType(typeName)
		if (!type) throw new IllegalArgumentException("no type registered under '${typeName}' in ${parent.name}.${name}")
		setType(type)
	}
	MBind getType() {
		if (!this.type)
			setType(MTypeRegistry.instance().VOID) //lazy init
		this.type
	}
	boolean isVoidType() { MTypeRegistry.instance().VOID == getType().type }
	/**
	 * body can ba a Closure or anything with a toString() method.
	 */
	void setBody(final body) {
		this.body = (body instanceof Closure) ? body : { MMethod m, CodeEmitter v, boolean hasSuper ->  v.out << body.toString() }
	}
	/**
	 * convenience method that sets single expression method body for Kotlin methods.
	 * Sets singleExpr=true and body=expr. expr can ba a Closure or anything with a toString() method.
	 */
	void setExpr(final expr) {
		singleExpr = true
		setBody(expr)
	}


}
