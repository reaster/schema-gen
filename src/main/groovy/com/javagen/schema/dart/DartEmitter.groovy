/*
 * Copyright (c) 2019 Outsource Cafe, Inc.
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

package com.javagen.schema.dart

import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.model.*
import com.javagen.schema.model.MMethod.Stereotype
import groovy.util.logging.Log

/**
 * Traverses model and emits Dart/Flutter code.
 *
 * Style reference: https://www.dartlang.org/guides/language/effective-dart
 * 
 * @author Richard Easterling
 */
@Log
class DartEmitter extends CodeEmitter
{
	boolean assignDefaultValues = true
	boolean mutableLists = true
	boolean openOnNL = false

	DartEmitter()
	{
		if ( ! MTypeRegistry.isInitialized() )
			new DartTypeRegistry()
	}

	/** supports grouping classes into single source file */
	@Override
	def visit(MModule m)
	{
		List<MClass> classes = m.classes.findAll{ c -> !c.ignore }
		if (m.isSource()) {
			log.info "OUTPUT: ${m.sourceFile}" //TODO weird bug - only prints on first pass
			openWriter(m.sourceFile)
			List<String> imports = m.gatherSourceImports()
			//out << 'package ' << m.fullName() << '\n'
			imports.each {
				out << '\n' << 'import \'package:' << it << '\';'
			}
			if (!imports.isEmpty())
				out << '\n'
			List<String> parts = m.gatherSourceParts()
			parts.each {
				out << '\n' << 'part \'' << it << '\';'
			}
			if (!parts.isEmpty())
				out << '\n'
			if (m.partOf)
				out << 'part of \'' << m.partOf << '\';\n'
		}
		classes.each { c -> //visit declared classes and interfaces
			if (!c.ignore) {
				visit(c)
				out << '\n'
			}
		}
		for(f in m.fields.values()) {
			visit(f)
		}
		m.methods.forEach{ f ->
			visit(f)
		}
		for(childModule in m.children.values()) { //visit submodules
			visit(childModule)
		}

		closeWriter()
	}

	@Override
	def visit(MClass c)
	{
		if (c.isSource()) {
			println "OUTPUT: ${m.sourceFile}"
			openWriter(c.sourceFile)
			List<String> imports = c.gatherSourceImports()
			//out << 'package ' << c.fullName() << '\n'
			imports.each {
				out << '\n' << 'import \'package:' << it << '\';'
			}
			if (!imports.isEmpty())
				out << '\n'
		}

		c.annotations.each {
			out << '\n' << tabs
			out << it
		}
		out << '\n' << tabs
		if (c.scope && c.scope != 'public')
			out << c.scope << ' '
		if (c.isStatic())
			out << 'static '
		if (c.isAbstract())
			out << 'abstract '
		out << (c.mixin ? 'mixin ' : (c.isEnum() ? '' : 'class ')) << c.name
		if (c.getExtends())
			out << ' extends ' << c.getExtends()
		c.implements.unique().eachWithIndex { e, i ->
			out << (i==0 ? ' implements ' : ', ')
			out << e
		}

		/*
		if (!c.fields.isEmpty()) {
			out << '('
			this++
			c.fields.values().eachWithIndex { MField f, int i ->
				if (i>0)
					out << ', '
				out << '\n' << tabs
				f.annotations.each {
					out << it << ' '
				}
				out << (f.isFinal() ? 'final ' : '')
				out << typeDeclaration(f) << ' ' << f.name
				def val = defaultValue(f, f.val)
				if (val != null) {
					out << ' = ' << val
				} else if (assignDefaultValues && f.cardinality == MCardinality.OPTIONAL) {
					out << ' = null'
				} else if (assignDefaultValues && f.cardinality == MCardinality.REQUIRED) {
					out << ' = ' << c.name << '()'
				}
			}
			this--
			out << '\n' << tabs << ')'
		}
		*/
		if (openOnNL) {
			out << '\n' << tabs << '{'
		} else {
			out << ' {'
		}
		this++
		for(f in c.fields.values()) {
			visit(f)
		}
		for(m in c.methods.findAll{it.stereotype == MMethod.Stereotype.constructor}) {
			visit(m)
		}
		if (!c.classes.isEmpty())
			out << '\n'
		for(nested in c.classes) {
			if (!nested.ignore) {
				visit(nested)
			}
		}
		for(m in c.methods.findAll{it.stereotype != MMethod.Stereotype.constructor}) {
			visit(m)
		}
		this--
		out << '\n' << tabs << '}'
	}

	@Override
	def visit(MEnum c)
	{
		if (c.isSource()) {
			openWriter(c.sourceFile)
			List<String> imports = c.gatherSourceImports()
			//out << 'package ' << c.fullName() << '\n'
			imports.each {
				out << '\n' << 'import \'package:' << it << '\';'
			}
			if (!imports.isEmpty())
				out << '\n'
		}

		c.annotations.each {
			out << '\n' << tabs
			out << it
		}
		out << '\n' << tabs
		if (c.scope && c.scope != 'public')
			out << c.scope << ' '
		if (c.isStatic())
			out << 'static '
		if (c.isAbstract())
			out << 'abstract '
		out << 'enum ' << c.name
		out << '\n' << tabs << '{'
		this++
		c.enumNames.eachWithIndex{ enumName, index ->
			if (c.annotationValues[index]) {
				out << '\n' << tabs << c.annotationValues[index]
			}
			out << '\n' << tabs << enumName
			out << (index==c.enumNames.size()-1 ? '' : ',')
		}
		for(m in c.methods) {
			visit(m)
		}
		this--
		out << '\n' << tabs << '}'
	}

	@Override
	def visit(MField f) {
		f.annotations.each {
			out << '\n' << tabs
			out << it
		}
		out << '\n' << tabs
		//if (f.scope)
		//	out << f.scope << ' '
		if (f.isConst())
			out << 'const '
		if (f.isStatic())
			out << 'static '
		if (f.isFinal())
			out << 'final '
		//def iType = f.type.name
		out << typeDeclaration(f) << ' ' << (f.scope == 'private' ? '_' : '') << f.name
		def val = defaultValue(f, f.val)
		if (val != null) {
			out << ' = ' << val
		}
		out << ';'
	}

	@Override
	def visit(MReference r)
	{
		visit( (MProperty)r )
	}
	
	@Override
	def visit(MProperty p)
	{
		visit( (MField)p )
	}

	@Override
	def visit(MMethod m)
	{
//		if ('_$enumDecodeNullable' == m.name)
//			println m.name
		if (!m.name && m.stereotype != Stereotype.constructor) {
			println "ignoring method with no name: ${m}"
			return
		}
		MClass c = m.parent instanceof MClass ? m.parent : null
		out << '\n' << '\n' << tabs
		boolean annotationsOnNL = m.annotations.size() > 1
		m.annotations.each {
			out << it
			if (annotationsOnNL) {
				out << '\n' << tabs
			} else {
				out << ' '
			}
		}
//		if (m.scope && m.scope != 'public')
//			out << m.scope << ' '
		if (m.factory)
			out << 'factory '
		if (m.override)
			out << 'override '
		if (m.isStatic())
			out << 'static '
		if (m.isFinal())
			out << 'final '
//		if (m.isAbstract())
//			out << 'abstract '
		if (m.stereotype == Stereotype.constructor) {
			boolean allProperties = MMethod.IncludeProperties.allProperties == m.includeProperties
			if (allProperties) {
				Map<String,MBind> localProperties = c.fields
				out << m.name << '({'
				this++
				m.params.eachWithIndex { MBind p, int i ->
					if (i>0) out << ', '
					boolean isLocal = localProperties.containsKey(p.name)
					out << '\n' << tabs << "${ isLocal ? 'this.' : '' }${p.name}"
					def val = defaultValue(p, p.val)
					if (val != null) {
						out << ' = ' << val
					}
				}
				this--
				out << '\n' << tabs << "})"
				Map<String,MBind> superProperties = c.inheritedFields(false)
				if (!superProperties.isEmpty()) {
					out << ' : super('
					this++
					superProperties.eachWithIndex { Map.Entry<String, MField> e, int i ->
						if (i>0) out << ', '
						out << '\n' << tabs << e.key << ':' << e.key
					}
					this--
					out << '\n' << tabs << ')'
				}
			} else {
				out << m.name << '('
				m.params.eachWithIndex { MBind p, int i ->
					if (i>0) out << ', '
					out << typeDeclaration(p, m) << ' ' << p.name
				}
				out << ')'
			}
//			out << "): this("
//			m.params.eachWithIndex { MBind p, int i ->
//				if (i>0) out << ', '
//				out << "${p.name}" << '=' << "${p.name}_"
//			}
		} else {
			if (!m.isVoidType()) {
				out << typeDeclaration(m.type, m) << ' '
			}
			out << (m.operator ? 'operator ' : '')
			out << (m.getter ? 'get ' : '')
			out << (m.setter ? 'set ' : '')
			out << m.name
			m.params.eachWithIndex { MBind p, int i ->
				if (p.type.generic) {
					out << '<' << p.type.name << '>'
				}
				if (p.cardinality == MCardinality.MAP && p.attr['keyType']?.generic) {
					out << '<' << p.attr['keyType'].name << '>'
				}
			}
			if (!m.getter) { //leave off parathesees on getters
				out << '('
				m.params.eachWithIndex { MBind p, int i ->
					if (i>0) out << ', '
					out << typeDeclaration(p, m) << ' ' << p.name
				}
				out << ')'
			}
//			if (m.stereotype != Stereotype.constructor && !m.voidType) {
//				out << ': ' << typeDeclaration(m.type, m)
//			}
		}
		if (m.isAbstract() || m.parent.isInterface() || m.body == null) {
			out << ';'
		} else if (m.body!=null) {
			if (m.singleExpr) {
				out << ' => '
				m.body(m, this, m.parent.hasSuper())
				out << ';'
			} else {
				out << ' {'
				this++
				m.body(m, this, m.parent.hasSuper())
				this--
				out << '\n' << tabs << '}'
			}
		} else {
			out << ' {'
			this++
			out << '\n' << tabs << ""//TODO generated ${m.stereotype?.name ?: m.name}"
			if (!m.isVoidType()) {
				final String val = defaultValue(m)
				out << '\n' << tabs << 'return ' << val << ';'
			}
			this--
			out << '\n' << tabs << '}'
		}
	}

	private String typeDeclaration(MBind p, MMethod m = null)
	{
		String container = DartTypeRegistry.container(p.cardinality,mutableLists)
		switch (p.cardinality) {
			case MCardinality.ARRAY:
			case MCardinality.LIST:
			case MCardinality.SET:
				boolean opt = p.optionalContainerType
				return "${container}<${p.type.name}>" //${opt ? '?' : ''}
			case MCardinality.OPTIONAL:
				return p.type.name // + '?'
			case MCardinality.MAP:
			case MCardinality.LINKEDMAP:
				def attr = p.attr
				def keyType = p.attr['keyType']
				keyType = keyType ?:  m?.refs['property']?.attr['keyType']
				keyType = keyType ?: 'String'
				return "${container}<${keyType},${p.type.name}>"
			default: // REQUIRED
				return p.type.name
		}
	}

	private String valueQuote(MBind f, String val)
	{
		if (val != null) {
			final String type = f.type.name
			if ('String' == type || 'CharSequence' == type || type.endsWith('StringBuilder')) {
				return '\"'
			} else if ('Char' == type) {
				return '\''
			}
		}
		return ''
	}

	String defaultValue(MField f)
	{
		defaultValue(f, f.val)
	}

	String defaultValue(MBind f, def val = null)
	{
		if (val!=null || f.isFinal()) //const case
			return genDefaultValue(f, val)
		if (!assignDefaultValues && MCardinality.OPTIONAL!=f.cardinality) //if not required to assign default values, don't
			return null
		//genDefaultValue(f, val) //otherswise defaults always required
	}
	String defaultValue(MMethod m)
	{
		genDefaultValue(m.type)
	}

	String genDefaultValue(MBind f, def val = null)
	{
		def container = DartTypeRegistry.container(f.cardinality, mutableLists)
		switch (f.cardinality) {
			case MCardinality.ARRAY:
			case MCardinality.LIST:
			case MCardinality.MAP:
			case MCardinality.LINKEDMAP:
			case MCardinality.SET:
				if (val)
					return val
				val = MTypeRegistry.instance().lookupDefaultValue(container)
				if (val)
					return val
				break
			case MCardinality.OPTIONAL:
				val = val ?: (f.type.val ?: null)
				if (f.type instanceof MEnum) {
					if (val)
						return "${f.type.name}.${gen.enumNameFunction.apply(val)}"
					else
						return null
				} else {
					final String quote = valueQuote(f, val)
					return (val==null) ? null : "${quote}${val}${quote}"
				}
			default: // REQUIRED
				val = val ?: (f.type.val ?: null)
				if (f.type instanceof MEnum) {
					if (val)
						return "${f.type.name}.${gen.enumNameFunction.apply(val)}"
					else
						return "${f.type.name}.Unknown" //TODO desperate
				} else {
					if (val==null) {
						val = MTypeRegistry.instance().lookupDefaultValue(f.type.name)
						if (val == 'null') { // return default constructor call
							return "${f.type.name}()"
						}
					}
					final String quote = valueQuote(f, val)
					return (val==null) ? null : "${quote}${val}${quote}"
				}
		}
		val
	}


}
