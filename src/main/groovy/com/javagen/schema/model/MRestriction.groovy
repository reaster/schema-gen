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
 * One size fits all restriction store, one or more of which can be applied to a MBind (addField, property, parameter or return type)
 * 
 * see http://java.sun.com/javaee/6/docs/api/index.html?javax/validation/constraints/package-tree.html
 * see http://stackoverflow.com/questions/925991/objective-c-nsstring-to-enum
 * @author richard
 */
class MRestriction extends MBase
{
	static enum Type {enummeration, min, minExclusive, max, maxExclusive, length, regexp, notnull}
	Type type
	Number number
	String regexp
	def enumNames = []
	def enumValues = []
	def enumDefault
	MRestriction(Type type, def value) {
		setValue(type, value)
	}
	private def setValue(Type type, def value) {
		this.type = type
		switch (type) {
			case Type.enummeration:
				enumNames = value
				break
			case Type.min:
			case Type.minExclusive:
			case Type.max:
			case Type.maxExclusive:
			case Type.length:
				number = toNumber(value)
				break
			case Type.regexp:
				regexp = value
				break
		}
	}
	def getValue() {
		switch (type) {
			case Type.enummeration:
				return enumNames
			case Type.min:
			case Type.minExclusive:
			case Type.max:
			case Type.maxExclusive:
			case Type.length:
				return number
			case Type.regexp:
				return regexp
			default: 
				return null
		}
	}
	static MRestriction find(List<MRestriction> list, Type type) { list.find { it.type == type } }
	static def findValue(List<MRestriction> list, Type type) { find(list, type)?.getValue() }

	private static Number toNumber(Object n)
	{
		if (n instanceof Number) {
			return n
		}
		try {
			return Integer.parseInt(n)
		} catch (NumberFormatException e) {
			try {
				return Double.parseDouble(n)
			} catch (NumberFormatException e2) {
				return null
			}
		}
	}
}
