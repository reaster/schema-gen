/*
 * Copyright (c) 2017 Outsource Cafe, Inc.
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

package com.javagen.schema.java

import com.javagen.schema.common.Gen
import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MTypeRegistry
import com.javagen.schema.common.GlobalFunctionsUtil

/**
 * Old template-based code generator supporting an in-house XML parser - currently not in use.
 *
 * @author Richard Easterling
 */
class Saxy4JVisitor extends CodeEmitter
{
    //TODO add template support for: wrapper types, root tags, 
    String XML_MAPPER_CLASS = '''package ${xmlPackageName};

import java.io.File;
import java.common.List;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import com.javagen.xml.XmlClassMapping;
import com.javagen.xml.XmlMapping;
import com.javagen.xml.XmlMarshaller;
import com.javagen.xml.XmlUnmarshaller;

public class ${xmlMapperClass} {

	protected XmlMapping ${mappingInstanceName};

	public XmlMapping getXmlMapping() 
	{
		if (${mappingInstanceName} == null) {
			${mappingInstanceName} = new XmlMapping.Builder()
				.setNamespace(\"${nsPrefix}\", "${nsURL}\")
				.setSchemaLocation(\"${schemaLocation}\")<% skipRootTags.each { root -> %>
				.skipRootTag(\"${root}\")<% } %>
				.setAllowRuntimeMapping(false)
				.build();<% classes.each { className, addClass -> %>
			${mappingInstanceName}.mapClass(new XmlClassMapping.Builder(${addClass.fullName()}.class)
				.mapAttributes(<% addClass.fieldsWithAttr('attribute').eachWithIndex { addField, i -> %>${(i>0 ? ', ':'')}\"${addField.name}\"<% } %>)<% addClass.fieldsWithAttr('element').each { addField -> if (addField.isContainerType()) { %>
				.mapContainer(\"${addField.attr['element']}\", \"${addField.name}\")<% } else { %> 
				.mapElement(\"${addField.attr['element']}\", \"${addField.name}\")<% } } %><% addClass.fieldsWithAttr('body').each { addField -> %>
				.mapTextBody(\"${addField.attr['body']}\")<% } %>
				.build());<% } %>
			<% globalElementTypes.each { tag, typeName -> %>
			${mappingInstanceName}.mapRootTag(\"${tag}\", mapping.getClassMapping(${typeName}.class));<% } %>
		}
		return ${mappingInstanceName};
	}
	
	public List<Object> unmarshalAsList(File xml, boolean stripBlanks) throws MalformedURLException, ParserConfigurationException, SAXException, IOException 
	{
		XmlUnmarshaller unmarshaller = new XmlUnmarshaller(getXmlMapping());
		unmarshaller.setRemoveBlanks(stripBlanks);
		List<Object> instances = unmarshaller.instances(xml.toURI().toURL());
		return instances;
	}
	public Object unmarshal(File xml, boolean stripBlanks) throws MalformedURLException, ParserConfigurationException, SAXException, IOException 
	{
		List<Object> instances = unmarshalAsList(xml, stripBlanks);
		return (instances == null || instances.size() == 0) ? null : instances.get(0);
	}
	public Object unmarshalXml(String xml, boolean stripBlanks) throws MalformedURLException, ParserConfigurationException, SAXException, IOException 
	{
		XmlUnmarshaller unmarshaller = new XmlUnmarshaller(getXmlMapping());
		unmarshaller.setRemoveBlanks(stripBlanks);
		List<Object> instances = unmarshaller.instances(xml);
		return instances != null && instances.size() > 0 ? instances.get(0) : null;
	}
	public Object unmarshalXml(InputSource inputSource, boolean stripBlanks) throws MalformedURLException, ParserConfigurationException, SAXException, IOException 
	{
		XmlUnmarshaller unmarshaller = new XmlUnmarshaller(getXmlMapping());
		unmarshaller.setRemoveBlanks(stripBlanks);
		List<Object> instances = unmarshaller.instances(inputSource);
		return instances != null && instances.size() > 0 ? instances.get(0) : null;
	}
	public String marshal(Object root) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException 
	{
		XmlMarshaller marshaller = new XmlMarshaller(getXmlMapping());
		String xml = marshaller.marshal(root);
		return xml;
	}
	public boolean marshal(Object root, File output) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException 
	{
		XmlMarshaller marshaller = new XmlMarshaller(getXmlMapping());
		return marshaller.marshal(root, output);
	}
}
'''

    Saxy4JVisitor(Gen gen, def out) {
        super()
        this.gen = gen
        this.out = out
        if ( ! MTypeRegistry.isInitialized() )
            new JavaTypeRegistry()
    }
    Saxy4JVisitor(Gen gen) { this(gen, new PrintWriter(System.out)) }

    @Override //TODO not tested, may need to look at old class: XmlSchemaCodeGenBase
    def visit(MModule m)
    {
        def xmlMapperClass = 'XmlTranslater'
        def mappingInstanceName = 'mapping'
        def templateBinding = gen.templateBindingFromProperties() + [xmlMapperClass:xmlMapperClass,mappingInstanceName:mappingInstanceName]
        def fullClassName = gen.packageName+'.'+xmlMapperClass
        File outputFile = new File(gen.srcDir, GlobalFunctionsUtil.pathFromPackage(fullClassName,'java'))
        gen.genTemplate(XML_MAPPER_CLASS, outputFile, templateBinding)
    }
 }
