package com.javagen.gen.model

class MBase {
	String name
	def attr = [:]
	def annotations = []
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
}
