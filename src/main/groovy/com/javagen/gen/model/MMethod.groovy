package com.javagen.gen.model

import com.javagen.gen.MVisitor

class MMethod extends MBase
{
	enum Stereotype {unknown, constructor, getter, setter, adder, putter, toString, equals, hash}
	enum IncludeProperties {noProperties, finalProperties, allProperties}
	Stereotype stereotype = Stereotype.unknown
	IncludeProperties includeProperties
	MClass parent
	def refs = [:]
	private MBind type
	List<MBind> params = []
	protected def scope = 'public'
	protected boolean _abstract
	protected boolean _static
	protected boolean _final
	boolean singleExpr = false
	boolean override = false
	/** signature takes two params: lambda(MMethod m, MVisitor v) and writes text to v.out */
	Closure body = null

	def MMethod() { }
	def MMethod(String name, MField field, Stereotype stereotype) {
		this.name = name
		refs['property'] = field
		this.stereotype = stereotype
	}
	MMethod setAbstract(a) { _abstract = a; return this }
	boolean isAbstract() { _abstract }
	MMethod setStatic(s) { _static = s; return this }
	boolean isStatic() { _static }
	MMethod setFinal(f) { _final = f; return this }
	boolean isFinal() { _final }
	void setType(MBind type) { this.type = type }
	void setType(MType type) { this.type = new MBind(type: type) }
	void setType(String typeName) {
		MType type = MType.lookupType(typeName);
		if (!type) throw new IllegalArgumentException("no type registered under '${typeName}'")
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
		this.body = (body instanceof Closure) ? body : { MMethod m, MVisitor v ->  v.out << body.toString() }
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
