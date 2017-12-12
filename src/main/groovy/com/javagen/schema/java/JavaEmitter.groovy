package com.javagen.schema.java

import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.model.MBind
import com.javagen.schema.model.MCardinality
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MField
import com.javagen.schema.model.MMethod
import com.javagen.schema.model.MMethod.Stereotype
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.model.MReference
import com.javagen.schema.model.MTypeRegistry

/**
 * Traverses model and emits Java code.
 * 
 * @author richard
 */
class JavaEmitter extends CodeEmitter
{
	boolean assignDefaultValues = false;
	//boolean useOptional = false;

	JavaEmitter()
	{
		if ( ! MTypeRegistry.isInitialized() )
			new JavaTypeRegistry()
	}


//	@Override
//	String fileName(MClass c) {
//		GlobalFunctionsUtil.pathFromPackage(c.fullName())
//	}
//
//	@Override
//	String fileName(String className) {
//		GlobalFunctionsUtil.pathFromPackage(className)
//	}
	
	@Override
	def visit(MModule m) {
		List<MClass> classes = m.classes.findAll{ c -> !c.ignore }
		classes.each { c -> //visit declared classes and interfaces
			File sourceFile = gen.classOutputFile.apply(gen,c)
			openWriter(sourceFile)
			out << 'package ' << m.fullName() << ';\n'
			c.imports.each {
				out << '\n' << 'import ' << it << ';'
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
		if (c.scope)
			out << c.scope << ' '
		if (c.isStatic())
			out << 'static '
		if (c.isAbstract())
			out << 'abstract '
		out << (c.isInterface() ? 'interface ' : 'class ') << c.name
		out << '\n' << tabs << '{'
		this++
		for(f in c.fields.values()) {
			visit(f)
		}
		if (!c.classes.isEmpty())
			out << '\n'
		for(nested in c.classes) {
			visit(nested)
		}
		if (!c.methods.isEmpty())
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
		if (c.scope)
			out << c.scope << ' '
		if (c.isStatic())
			out << 'static '
		if (c.isAbstract())
			out << 'abstract '
		out << 'enum ' << c.name
		out << '\n' << tabs << '{'
		this++
		c.enumNames.eachWithIndex{ enumName, index ->
			out << '\n' << tabs << enumName
			if (!c.enumValues.isEmpty()) {
				out << '(\"' << c.enumValues[index] << '\")'
			}
			out << (index==c.enumNames.size()-1 ? ';' : ',')
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
	def visit(MField f) {
		f.annotations.each {
			out << '\n' << tabs
			out << it
		}
		out << '\n' << tabs
		if (f.scope)
			out << f.scope << ' '
		if (f.isStatic())
			out << 'static '
		if (f.isFinal())
			out << 'final '
		def iType = f.type.name
		out << typeDeclaration(f) << ' ' << f.name
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
		if (!m.name) {
			println "ignoring method with no name: ${m}"
			return
		}
		m.annotations.each {
			out << '\n' << tabs
			out << it
		}
		out << '\n' << tabs
		if (m.scope)
			out << m.scope << ' '
		if (m.isStatic())
			out << 'static '
		if (m.isFinal())
			out << 'final '
		if (m.isAbstract())
			out << 'abstract '
		if (m.stereotype != Stereotype.constructor) {
			out << typeDeclaration(m.type, m) << ' '
		}
		out << m.name << '('
		m.params.eachWithIndex { MBind p, int i ->
			if (i>0) out << ', '
			out << typeDeclaration(p, m) << ' ' << p.name
		}
		out << ')'
		if (m.isAbstract() || m.parent.isInterface()) {
			out << ';'
		} else if (m.body!=null) {
			out << ' {'
			this++
			m.body(m, this)
			this--
			out << '\n' << tabs << '}'
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
		def container = JavaTypeRegistry.containerInterface(p.cardinality)
		switch (p.cardinality) {
			case MCardinality.ARRAY:
				return p.type.name + '[]'
			case MCardinality.LIST:
			case MCardinality.SET:
			case MCardinality.OPTIONAL:
				return container + '<' + p.type.name + '>'
			case MCardinality.MAP:
				def keyType = p.attr['keyType']
				keyType = keyType ?:  m?.refs['property']?.attr['keyType']
				keyType = keyType ?: 'String'
				return container + '<' + keyType + ',' + p.type.name + '>'
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
			} else if ('Character' == type || 'char' == type) {
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
		genDefaultValue(f, val) //otherswise defaults always required
	}
	String defaultValue(MMethod m)
	{
		genDefaultValue(m.type)
	}

	String genDefaultValue(MBind f, def val = null)
	{
		def container = JavaTypeRegistry.containerInterface(f.cardinality)
		switch (f.cardinality) {
			case MCardinality.ARRAY:
				return "new ${f.type.name}[0]"
			case MCardinality.LIST:
			case MCardinality.MAP:
			case MCardinality.SET:
				val = MTypeRegistry.instance().lookupDefaultValue(container)
				if (val)
					return val
				break
			case MCardinality.OPTIONAL:
				if (!(f.type instanceof MEnum)) {
					final String quote = valueQuote(f, val)
					return (val==null) ? "${container}.empty()" : "${container}.of(${quote}${val}${quote})"
				}
			default: // REQUIRED
				val = val ?: (f.type.val ?: null)
		}
		if (val==null)
			return null
		if (f.type instanceof MEnum) {
			val = "${f.type.name}.${gen.enumNameFunction.apply(val)}"
		}
		String quote = valueQuote(f, (String)val)
		"${quote}${val}${quote}"
	}



//	String JAVA_ENUM_CLASS = '''<% if (packageName) { %>package ${packageName};
//
//<% } %>/** Enumeration generated from XML xml enumeration restriction. */
//${tab}public enum ${className}
//${tab}{
//    <% enumInstanceNames.eachWithIndex { className, i -> %>${tab}${className}(\"${enumValueNames[i]}\")${(i==enumInstanceNames.size()-1) ? ';' : ','}
//<% } %>
//${tab}    private final String value;
//${tab}    private ${className}(String v) { value = v; }
//${tab}    public String value() { return value; }
//
//${tab}    public static ${className} fromValue(String value) {
//${tab}    	final String v = value.intern();
//${tab}        for (${className} e: ${className}.values()) {
//${tab}            if (e.value == v) {
//${tab}                return e;
//${tab}            }
//${tab}        }
//${tab}        <% if(unknownEnum) { %>return ${unknownEnum};<% } else { %>throw new IllegalArgumentException(this.getClass().getSimpleName()+\"does not recognize enum value: \"+v);<% } %>
//${tab}    }
//${tab}}'''

}
