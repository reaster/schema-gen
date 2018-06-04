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

class MBase {
	String name
	def attr = [:]
	Annotations annotations = new Annotations()
	static final String TEMPLATE_NAME = 'TEMPLATE_NAME'

	String shortName() { 
		int pos = name ? name.lastIndexOf('.') : -1
		return pos >= 0 ? name.substring(pos+1) : name
	}
	String fullName() { 
		return name
	}
	String getTemplateName() { attr[TEMPLATE_NAME] }
	def setTemplateName(String template) { attr[TEMPLATE_NAME] = template }

	String nestedAttr(String key)
	{
		return attr[key]
	}

	/**
	 * Allows strings to be passed in, as well as, MAnnotation types.
	 */
	static class Annotations
	{
		List<MAnnotation> list = []
		def leftShift(item) {
			if ((item instanceof MAnnotation)) {
				list << item
			} else {
				list << new MAnnotation(expr:item.toString())
			}
		}
		boolean isEmpty() { list.isEmpty() }
		def each(Closure c) { list.each(c) }
		int size() { list.size() }
		boolean startsWith(String annotationPrefix)
		{
			list.find{ it.toString().startsWith(annotationPrefix) }
		}
		boolean contains(String annotation)
		{
			list.find{ it.toString() == annotation }
		}
		//def findAll(Closure c) { list.findAll{c} }
	}

}
