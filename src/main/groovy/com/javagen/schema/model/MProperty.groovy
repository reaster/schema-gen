package com.javagen.schema.model

class MProperty extends MField
{
	Map<MMethod.Stereotype,MMethod> methods = [:]
	Closure getterBody = null
	Closure setterBody = null

	MProperty() {
		super()
	}
}
