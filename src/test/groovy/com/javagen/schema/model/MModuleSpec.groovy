package com.javagen.schema.model

import com.javagen.schema.common.Gen
import com.javagen.schema.java.SchemaToJava
import spock.lang.Shared
import spock.lang.Specification

import static com.javagen.schema.xml.node.Schema.getDEFAULT_NS

class MModuleSpec extends Specification
{
    def "fileName on class vs modules"()
    {
        given:
        Gen gen = new SchemaToJava()
        MModule m1 = new MModule(name: 'm1')
        MClass c1 = new MClass(name:'C1')
        m1.addClass(c1)
        when:
        File f1 = gen.classOutputFileFunction.apply(gen, c1)
        then:
        f1 != null
        f1.toString().endsWith('src/main/java-gen/m1/C1.java')
//        when:
//        //gen.classOutputFileFunction = { gen_,clazz -> Gen.fileNmeFromAttr(gen_, clazz, 'fileName', false) }
//        f1 = gen.classOutputFileFunction.apply(gen, c1)
//        then:
//        f1.toString().endsWith('NoFileNameAttrSet.java')
//        when:
//        m1.attr['fileName'] = 'S'
//        f1 = gen.classOutputFile.apply(gen, c1)
//        then:
//        f1.toString().endsWith('S.java')
//        when:
//        c1.attr['fileName'] = 'X'
//        f1 = gen.classOutputFile.apply(gen, c1)
//        then:
//        f1.toString().endsWith('X.java')
    }

}
