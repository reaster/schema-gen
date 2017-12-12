package com.javagen.schema.common

import com.javagen.schema.common.Gen
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MField
import com.javagen.schema.model.MMethod
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.model.MReference
import groovy.text.SimpleTemplateEngine

/**
 * Base class for code generator emitter. Subclasses should override visitor methods as needed.
 *
 * @author richard
 */
class CodeEmitter
{
	Gen gen
	def out = new PrintWriter(System.out)
	protected def templateEngine
	protected def templateCache = [:]
	def tabSize = 4
	def indent = 0
	def tabs = ''
	def types = [:]
	File openFile = null
	Closure<PrintWriter> openStreamLambda = { File file -> new PrintWriter(file) }

	//entry point
	//def xml() { visit( xml.getModel() ) }

	//noop visitor methods:
	def visit(MModule m) {}
	def visit(MClass c) {}
	def visit(MEnum e) {}
	def visit(MField f) {}
	def visit(MProperty p) {}
	def visit(MReference r) {}
	def visit(MMethod m) {}

	/**
	 *
	 * @param sourceFile
	 * @return
	 */
	def openWriter(File sourceFile) {
		if (!sourceFile.equals(openFile)) {
			if (out)
				out.close()
			try {
				out = openStreamLambda( sourceFile )
			} catch(FileNotFoundException ) { // mkdirs and try again
				sourceFile.parentFile.mkdirs()
				out = openStreamLambda( sourceFile )
			}
			openFile = sourceFile
			println '>'+sourceFile.toString()
		}
	}

	/**
	 * Only close if MSource is a MClass (i.e. has an assigned srcDir file) and the OutputStream is open.
	 */
	def closeWriter() {
		if (gen.srcDir && out) {
			out.close()
			out = null
			openFile = null
		}
	}

	//template handling
	def template(def templateText, String className, def binding) {
		if (!templateEngine)
			templateEngine = new SimpleTemplateEngine()
		def template = templateEngine.createTemplate(templateText)
		openWriter(className) //TODO fix me
		out << template.make(binding)
		closeWriter()
	}
	def template(def templateText, MClass c, def binding) { template(templateText, c.fullName(), binding) }
		
	//source code indentation
	def next() {
		indent++
		this.tabs = ' ' * (indent*tabSize)
	}
	def previous() {
		if (indent > 0)
			indent--
		this.tabs = ' ' * (indent*tabSize)
	}
}