package com.javagen.gen.java

import com.javagen.gen.Gen
import com.javagen.gen.MVisitor
import com.javagen.gen.model.MModule
import com.javagen.gen.model.MTypeRegistry
import com.javagen.gen.util.GlobalFunctionsUtil

class Saxy4JVisitor extends MVisitor
{
    //TODO add template support for: wrapper types, root tags, 
    String XML_MAPPER_CLASS = '''package ${xmlPackageName};

import java.io.File;
import java.util.List;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import javax.schema.parsers.ParserConfigurationException;
import org.schema.sax.InputSource;
import org.schema.sax.SAXException;
import com.javagen.schema.XmlClassMapping;
import com.javagen.schema.XmlMapping;
import com.javagen.schema.XmlMarshaller;
import com.javagen.schema.XmlUnmarshaller;

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
	
	public List<Object> unmarshalAsList(File schema, boolean stripBlanks) throws MalformedURLException, ParserConfigurationException, SAXException, IOException 
	{
		XmlUnmarshaller unmarshaller = new XmlUnmarshaller(getXmlMapping());
		unmarshaller.setRemoveBlanks(stripBlanks);
		List<Object> instances = unmarshaller.instances(schema.toURI().toURL());
		return instances;
	}
	public Object unmarshal(File schema, boolean stripBlanks) throws MalformedURLException, ParserConfigurationException, SAXException, IOException 
	{
		List<Object> instances = unmarshalAsList(schema, stripBlanks);
		return (instances == null || instances.size() == 0) ? null : instances.get(0);
	}
	public Object unmarshalXml(String schema, boolean stripBlanks) throws MalformedURLException, ParserConfigurationException, SAXException, IOException 
	{
		XmlUnmarshaller unmarshaller = new XmlUnmarshaller(getXmlMapping());
		unmarshaller.setRemoveBlanks(stripBlanks);
		List<Object> instances = unmarshaller.instances(schema);
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
		String schema = marshaller.marshal(root);
		return schema;
	}
	public boolean marshal(Object root, File output) throws IOException, IllegalArgumentException, IllegalAccessException, InvocationTargetException 
	{
		XmlMarshaller marshaller = new XmlMarshaller(getXmlMapping());
		return marshaller.marshal(root, output);
	}
}
'''

    Saxy4JVisitor(Gen gen, def out) {
        super(gen, out)
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
