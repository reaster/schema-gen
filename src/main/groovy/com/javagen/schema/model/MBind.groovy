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
 * A bound type has a scope, optional cardinality, optional name and optional restrictions.
 * It serves as a base class for field, property, return types and function parameter declarations.
 * 
 * @author richard
 */
class  MBind extends MBase
{
	MType type
	MCardinality cardinality = MCardinality.REQUIRED
	MRestriction[] restrictions = []
	private boolean _final
	MBind setFinal(f) { _final = f; return this }
	boolean isFinal() { _final }
	boolean isOptional() { cardinality ==  MCardinality.OPTIONAL || isOptionalContainerType() }
	String toString() { (isFinal() ? 'final ' : '')+(name ? name+' ' : '')+(cardinality == MCardinality.REQUIRED) ? type : cardinality.toString()+'<'+type+'>' }
	boolean isArray() { (cardinality ==  MCardinality.ARRAY) }
	boolean isContainerType() { (cardinality != MCardinality.REQUIRED) }
	boolean isOptionalContainerType() {
		def min = getRestrictionValue(MRestriction.Type.min)
		isContainerType() && min == 0
	}
	boolean isContainerType(EnumSet<MCardinality> set) { return set.contains(cardinality) }
	void setType(String typeName) { type = MType.lookupType(typeName); if (!type) throw new IllegalArgumentException("no type registered under '${typeName}'") }
	void setType(MType type) { this.type=type }
	MType getType() { return type }
	MRestriction getRestriction(MRestriction.Type type) { restrictions.find { r -> r.type == type }}
	Object getRestrictionValue(MRestriction.Type type) { getRestriction(type)?.getValue() }
	boolean isReference() {
		type ?
				!type.isBuiltIn()
				: false
	}
}
