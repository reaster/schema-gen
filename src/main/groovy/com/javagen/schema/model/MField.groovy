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
/**
 * A field type has a scope, optional value, parent class and import statement management.
 *
 * @author richard
 */
class MField extends MBind
{
	String scope
	def val
	private boolean _static
	/** set true to have code generator ignore this field or property */
	boolean genIgnore = false
	def parent
	private Imports imports = new Imports(this)

	MField() {
		super()
		type = MType.lookupType('String')
		scope = 'private'
	}
	MField setStatic(s) { _static = s; return this }
	boolean isStatic() { _static }
	MField setScope(String scope) { this.scope = scope; return this }
	def getImports() { imports }
	@Override String toString() {
		String cPre  = cardinality==MCardinality.OPTIONAL || !cardinality.container ? '' : "${cardinality.name()}<"
		String cPost = cardinality==MCardinality.OPTIONAL ? '?' : cardinality.container? '>' : ''
		String mapKey = cardinality==MCardinality.MAP ? "${attr['keyType']}," : ''
				"${(isStatic() ? 'static ' : '')}${(scope ? scope+' ' : '')}${cPre}${mapKey}${type?.name}${cPost} ${name}${(val ? ' = '+val : '')}"
	}

	/**
	 * passes imports down to base classes
	 */
	static class Imports
	{
		Set<String> list = [] as Set
		def owner
		Imports(owner) { this.owner=owner }
		def leftShift(item) {
			if ((owner.parent instanceof MModule)) {
				list << item
			} else {
				owner.parent.imports << item
			}
		}
		boolean isEmpty() { list.isEmpty() }
		def each(Closure c) { list.each(c) }
	}

}
