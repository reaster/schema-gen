package com.javagen.schema.kotlin

import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.model.*
import com.javagen.schema.model.MMethod.Stereotype

/**
 * Traverses model and emits Kotlin code.
 * 
 * @author richard
 */
class KotlinEmitter extends CodeEmitter
{
	boolean assignDefaultValues = true
	boolean mutableLists = true

	KotlinEmitter()
	{
		if ( ! MTypeRegistry.isInitialized() )
			new KotlinTypeRegistry()
	}
	
	@Override
	def visit(MModule m) {
		List<MClass> classes = m.classes.findAll{ c -> !c.ignore }
		if (m.isSource()) {
			//File sourceFile = xml.classOutputFile.apply(xml,c)
			openWriter(m.sourceFile)
			List<String> imports = m.gatherSourceImports()
			out << 'package ' << m.fullName() << '\n'
			imports.each {
				out << '\n' << 'import ' << it
			}
			if (!imports.isEmpty())
				out << '\n'
		}
		classes.each { c -> //visit declared classes and interfaces
			visit(c)
			out << '\n'
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
			openWriter(c.sourceFile)
			List<String> imports = c.gatherSourceImports()
			out << 'package ' << c.fullName() << '\n'
			imports.each {
				out << '\n' << 'import ' << it
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
		if (c.data)
			out << 'data '
		out << (c.isInterface() ? 'interface ' : 'class ') << c.name
		if (!c.fields.isEmpty()) {
			out << '('
			this++
			c.fields.values().eachWithIndex { MField f, int i ->
				if (i>0)
					out << ', '
				out << '\n' << tabs
				f.annotations.each { String a ->
					out << a << ' '
				}
				out << (f.isFinal() ? 'val ' : 'var ')
				out << f.name  << ':' << typeDeclaration(f)
				def val = defaultValue(f, f.val)
				if (val != null) {
					out << ' = ' << val
				} else if (assignDefaultValues && f.cardinality == MCardinality.OPTIONAL) {
					out << ' = null'
				} else if (assignDefaultValues && f.cardinality == MCardinality.REQUIRED) {
					out << ' = null'
				}
			}
			this--
			out << '\n' << tabs << ')'
		}
		out << '\n' << tabs << '{'
		this++
		for(m in c.methods.findAll{it.stereotype == MMethod.Stereotype.constructor}) {
			visit(m)
		}
		if (!c.classes.isEmpty())
			out << '\n'
		for(nested in c.classes) {
			visit(nested)
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
			out << 'package ' << c.fullName() << '\n'
			imports.each {
				out << '\n' << 'import ' << it
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
		out << 'enum class ' << c.name
		if (!c.fields.isEmpty()) {
			out << '('
			c.fields.values().eachWithIndex { MField f, int i ->
				if (i>0)
					out << ', '
				f.annotations.each { String a ->
					out << a << ' '
				}
				out << (f.isFinal() ? 'val ' : 'var ')
				out << f.name  << ':' << typeDeclaration(f)
			}
			out << ')'
		}
		out << '\n' << tabs << '{'
		this++
		c.enumNames.eachWithIndex{ enumName, index ->
			out << '\n' << tabs << enumName
			if (!c.enumValues.isEmpty()) {
				out << '(\"' << c.enumValues[index] << '\")'
			}
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
		if (f.scope)
			out << f.scope << ' '
		if (f.isStatic())
			out << 'static '
		if (f.isFinal())
			out << 'final '
		//def iType = f.type.name
		out << f.name << ': ' << typeDeclaration(f)
		def val = defaultValue(f, f.val)
		if (val != null) {
			out << ' = ' << val
		}
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
		if (!m.name && m.stereotype != Stereotype.constructor) {
			println "ignoring method with no name: ${m}"
			return
		}
		out << '\n' << tabs
		boolean annotationsOnNL = m.annotations.size() > 1
		m.annotations.each {
			out << it
			if (annotationsOnNL) {
				out << '\n' << tabs
			} else {
				out << ' '
			}
		}
		if (m.scope && m.scope != 'public')
			out << m.scope << ' '
		if (m.override)
			out << 'override '
		if (m.isStatic())
			out << 'static '
		if (m.isFinal())
			out << 'final '
		if (m.isAbstract())
			out << 'abstract '
		if (m.stereotype == Stereotype.constructor) {
			//assume named constructor
			out << 'constructor('
			m.params.eachWithIndex { MBind p, int i ->
				if (i>0) out << ', '
				out << "${p.name}_" << ':' << typeDeclaration(p, m)
			}
			out << "): this("
			m.params.eachWithIndex { MBind p, int i ->
				if (i>0) out << ', '
				out << "${p.name}" << '=' << "${p.name}_"
			}
			out << ")"
		} else {
			out << 'fun ' << m.name << '('
			m.params.eachWithIndex { MBind p, int i ->
				if (i>0) out << ', '
				out << p.name << ':' << typeDeclaration(p, m)
			}
			out << ')'
			if (m.stereotype != Stereotype.constructor && !m.voidType) {
				out << ': ' << typeDeclaration(m.type, m)
			}
		}
		if (m.isAbstract() || m.parent.isInterface()) {
			out << ';'
		} else if (m.body!=null) {
			if (m.singleExpr) {
				out << ' = '
				m.body(m, this)
			} else {
				out << ' {'
				this++
				m.body(m, this)
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
		String container = KotlinTypeRegistry.container(p.cardinality,mutableLists)
		switch (p.cardinality) {
			case MCardinality.ARRAY:
			case MCardinality.LIST:
			case MCardinality.SET:
				boolean opt = p.optionalContainerType
				return "${container}<${p.type.name}>${opt ? '?' : ''}"
			case MCardinality.OPTIONAL:
				return p.type.name + '?'
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
		genDefaultValue(f, val) //otherswise defaults always required
	}
	String defaultValue(MMethod m)
	{
		genDefaultValue(m.type)
	}

	String genDefaultValue(MBind f, def val = null)
	{
		def container = KotlinTypeRegistry.container(f.cardinality, mutableLists)
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
					}
					final String quote = valueQuote(f, val)
					return (val==null) ? null : "${quote}${val}${quote}"
				}
		}
		val
//		if (f.type instanceof MEnum) {
//			val = "${f.type.name}.${xml.enumNameFunction.apply(val)}"
//		}
//		String quote = valueQuote(f, (String)val)
//		"${quote}${val}${quote}"
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
