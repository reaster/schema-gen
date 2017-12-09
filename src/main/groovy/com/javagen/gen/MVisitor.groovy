package com.javagen.gen

import com.javagen.gen.model.MEnum
import com.javagen.gen.model.MClass;
import com.javagen.gen.model.MField;
import com.javagen.gen.model.MMethod;
import com.javagen.gen.model.MModule;
import com.javagen.gen.model.MProperty;
import com.javagen.gen.model.MReference;

/**
 * Base class for code generator emitter. Subclasses should override visitor methods as needed.
 *
 * @author richard
 */
class MVisitor
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
	//File srcDir

//	MVisitor(Gen gen, def out) { this.gen = gen; this.out = out }
//	MVisitor(Gen gen) { this(gen, ) }

	//entry point
	//def gen() { visit( gen.getModel() ) }

	//noop visitor methods:
	def visit(MModule m) {}
	def visit(MClass c) {}
	def visit(MEnum e) {}
	def visit(MField f) {}
	def visit(MProperty p) {}
	def visit(MReference r) {}
	def visit(MMethod m) {}

	//language-specific methods
//	String fileName(MClass c) { throw new UnsupportedOperationException("implement me!") }
//	String fileName(String className) { throw new UnsupportedOperationException("implement me!") }

	//file handling
//	def openWriter(String className) {
//		if (gen.srcDir) {
//			def relativePath = fileName(className)
//			def path = new File(gen.srcDir, relativePath)
//			try {
//				out = new PrintWriter( path )
//			} catch(FileNotFoundException e) { // mkdirs and try again
//				path.parentFile.mkdirs()
//				out = new PrintWriter( path )
//			}
//			println '>'+path.toString()
//		}
//	}
	//file handling
	def openWriter(File sourceFile) {
		if (!sourceFile.equals(openFile)) {
			if (out)
				out.close()
			try {
				out = new PrintWriter( sourceFile )
			} catch(FileNotFoundException e) { // mkdirs and try again
				sourceFile.parentFile.mkdirs()
				out = new PrintWriter( sourceFile )
			}
			openFile = sourceFile
			println '>'+sourceFile.toString()
		}
	}
	//def openWriter(MClass c) { openWriter(c.fullName()) }

	def closeWriter() {
		if (gen.srcDir && out) {
			out.close()
			out = null
			openFile = null
		}
	}
//	def closeWriter() {
//		if (gen.srcDir && out) {
//			out.close()
//			out = null
//		}
//	}

	//template handling
	def template(def templateText, String className, def binding) {
		if (!templateEngine)
			templateEngine = new groovy.text.SimpleTemplateEngine() 
		def template = templateEngine.createTemplate(templateText)
		openWriter(className)
		out << template.make(binding)
		closeWriter()
	}
	def template(def templateText, MClass c, def binding) { template(templateText, c.fullName(), binding) }
		
	//source code indentation
	def next() {
		indent++;
		this.tabs = ' ' * (indent*tabSize)
	}
	def previous() {
		if (indent > 0)
			indent--
		this.tabs = ' ' * (indent*tabSize)
	}
}