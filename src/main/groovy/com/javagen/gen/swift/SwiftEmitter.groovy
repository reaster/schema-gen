package com.javagen.gen.swift

import com.javagen.gen.MVisitor
import com.javagen.gen.model.MBind
import com.javagen.gen.model.MCardinality
import com.javagen.gen.model.MClass
import com.javagen.gen.model.MEnum
import com.javagen.gen.model.MField
import com.javagen.gen.model.MMethod
import com.javagen.gen.model.MMethod.Stereotype
import com.javagen.gen.model.MModule
import com.javagen.gen.model.MProperty
import com.javagen.gen.model.MReference
import com.javagen.gen.model.MTypeRegistry


/**
 * Traverses model and emits Swift 4 code.
 * 
 * @author richard
 */
class SwiftEmitter extends MVisitor
{

	boolean assignDefaultValues = true;

	SwiftEmitter()
	{
		if ( ! MTypeRegistry.isInitialized() )
			new SwiftTypeRegistry()
	}

	@Override
	def visit(MModule m)
	{
		List<MClass> classes = m.classes.findAll{ c -> !c.ignore }
		classes.each { c -> //visit declared classes and interfaces
			File sourceFile = gen.classOutputFile.apply(gen, c)
			openWriter(sourceFile)
			c.imports.each {
				out << '\n' << 'import ' << it
			}
			if (!c.imports.isEmpty())
				out << '\n'
			visit(c)
		}
		for(childModule in m.children.values()) { //visit submodules
			visit(childModule)
		}
		closeWriter()
	}

	@Override
	def visit(MClass c)
	{
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
		if (c.isExtension()) {
			out << 'extension '
		} else if (c.isStruct()) {
			out << 'struct '
		} else {
			out << (c.isInterface() ? 'protocol ' : 'class ')
		}
		out << c.name
		c.implements.eachWithIndex { String entry, int i ->
			out << (i==0 ? ': ' : ', ')
			out << entry
		}
		out << '\n' << tabs << '{'
		this++
		for(f in c.fields.values()) {
			visit(f)
		}
		if (!c.fields.isEmpty())
			out << '\n'
		for(nested in c.classes) {
			visit(nested)
		}
		if (!c.classes.isEmpty())
			out << '\n'
		for(m in c.methods) {
			visit(m)
		}
		this--
		out << '\n' << tabs << '}'
	}

	@Override
	def visit(MEnum c)
	{
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
		c.implements.eachWithIndex { String entry, int i ->
			out << (i==0 ? ': ' : ', ')
			out << entry
		}
		out << '\n' << tabs << '{'
		this++
		c.enumNames.eachWithIndex{ enumName, index ->
			out << '\n' << tabs << 'case ' << enumName
			if (!c.enumValues.isEmpty()) {
				out << ' = \"' << c.enumValues[index] << '\"'
			}
		}
		for(f in c.fields.values()) {
			visit(f)
		}
		if (!c.fields.isEmpty())
			out << '\n'
		for(m in c.methods) {
			visit(m)
		}
		this--
		out << '\n' << tabs << '}'
	}

	@Override
	def visit(MField f)
	{
	}

	@Override
	def visit(MReference r)
	{
		visit( (MProperty)r )
	}
	
	@Override
	def visit(MProperty f)
	{
		f.annotations.each {
			out << '\n' << tabs
			out << it
		}
		out << '\n' << tabs
		if (f.scope && f.scope != 'public')
			out << f.scope << ' '
		if (f.isStatic())
			out << 'static '
		out << (f.isFinal() ? 'let ' : 'var ')
		out << f.name << ': ' << typeDeclaration(f)
		if (f.getterBody && !f.setterBody) {
			out << ' {'
			this++
			f.getterBody(f, this)
			this--
			out << '\n' << tabs << '}'
		} else {
			String defaultValue = defaultValue(f)
			if (defaultValue!=null)
				out << ' = ' << defaultValue
		}
	}

	@Override
	def visit(MMethod m)
	{
		m.annotations.each {
			out << '\n' << tabs
			out << it
		}
		//if (!m.returnType) m.returnType = new MBind(type:MType.lookupType('Void'))
		out << '\n' << tabs
		if (m.scope && m.scope != 'public')
			out << m.scope << ' '
		if (m.isAbstract())
			out << 'abstract '
		if (m.isStatic())
			out << 'static '
		if (m.stereotype == Stereotype.constructor) {
			//def returnType = containerType ? containerType+'<'+m.returnType.type.name+'>' : m.returnType.type.name
			out << 'init'
		} else {
			out << 'func ' << m.name
		}
		out << '('
		m.params.eachWithIndex { MBind p, int i ->
			if (i>0) out << ', '
			out << p.name << ': ' << typeDeclaration(p, m)
		}
		out << ')'
		if (!m.isVoidType()) {
			out << ' -> ' << typeDeclaration(m.type, m)
		}
		if (m.isAbstract() || m.parent.isInterface()) {
			out << ''
		} else if (m.body) {
			out << ' {'
			this++
			m.body(m, this)
			this--
			out << '\n' << tabs << '}'
		} else {
			out << ' {\n'
			this++
			out << tabs << '//TODO\n'
			if (!m.isVoidType()) {
				out << '\n' << tabs << 'return ' << defaultValue(m.type)
			}
			this--
			out << tabs << '}'
		}
	}


	private String valueQuote(MBind f, String val)
	{
		if (val != null) {
			if ('String' == f.type.name || 'Character' == f.type.name) {
				return '"'
			}
		}
		return ''
	}

	private String typeDeclaration(MBind f, MMethod m = null)
	{
		switch (f.cardinality) {
			case MCardinality.ARRAY:
			case MCardinality.LIST: //var phone: [Phone]
				return '[' << f.type.name << ']'
			case MCardinality.MAP:
				def keyType = f.attr['keyType']
				keyType = keyType ?:  m?.refs['property']?.attr['keyType']
				keyType = keyType ?: 'String'
				return '[' << keyType + ':' + f.type.name + ']'
			case MCardinality.SET:
				def cType = SwiftTypeRegistry.containerClassName(f.cardinality)
				return cType + '<' + f.type.name + '>'
			case MCardinality.OPTIONAL:
				return f.type.name + '?'
			default: // REQUIRED
				return f.type.name
		}
	}

	String defaultValue(MField f)
	{
		defaultValue(f, f.val)
	}

	String defaultValue(MBind f, def val = null)
	{
		if (val!=null || f.isFinal()) {
			final String valQuote = valueQuote(f, (String)val)
			return f.cardinality.isContainer() ? val : "${valQuote}${val}${valQuote}"
		}
		if (!assignDefaultValues)
			return null
		def containerClass = SwiftTypeRegistry.containerClassName(f.cardinality)
		switch (f.cardinality) {
			case MCardinality.ARRAY:
			case MCardinality.LIST:
			case MCardinality.MAP:
			case MCardinality.SET:
				return MTypeRegistry.instance().lookupDefaultValue(containerClass)
			case MCardinality.OPTIONAL:
				if (val==null)
					return null
				final String valQuote = valueQuote(f, val)
				return "${valQuote}${val}${valQuote}"
			default: // REQUIRED
				val = f.type.val
		}
		final String valQuote = valueQuote(f, val)
		"${valQuote}${val}${valQuote}"
	}


}
