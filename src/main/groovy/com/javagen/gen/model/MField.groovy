package com.javagen.gen.model
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
	MClass parent
	private Imports imports = new Imports(this)

	def MField() {
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
