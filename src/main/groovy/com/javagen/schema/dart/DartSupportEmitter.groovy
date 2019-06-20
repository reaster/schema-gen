/*
 * Copyright (c) 2019 Outsource Cafe, Inc.
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

package com.javagen.schema.dart

import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.dart.DartTypeRegistry
import com.javagen.schema.model.*

import static com.javagen.schema.model.MMethod.Stereotype.fromJson
import static com.javagen.schema.model.MMethod.Stereotype.toJson

/**
 * Removes support code (currently just enums) from main module into a support 'part' module.
 *
 * @author Richard Easterling
 */
class DartSupportEmitter extends CodeEmitter
{

    MModule support = new MModule(name:'support')
    //boolean genEnumDecode = false

    DartSupportEmitter()
    {
         if ( ! MTypeRegistry.isInitialized() )
            new DartTypeRegistry()
    }

    @Override
    def visit(MModule m) {
        //println "MModule: ${m.name}"
        if (m.isRoot()) {
            m.child(support)
            support.sourceFile = DartUtil.toGeneratedSourceFileName(m.sourceFile, '.type')
            String name = m.sourceFile.name
            String importName = gen.projectPath(support.sourceFile) //toGeneratedRelativeFileName(gen.srcFolder, m, name)
            m.getImports() << importName
            //support.partOf = m.sourceFile.name
        }
        def enums = m.classes.findAll { c -> c instanceof MEnum }
        enums.each {
            m.classes.remove(it)
            support.addClass(it)
            //genEnumTable(it)
        }
    }


//    private genEnumTable(MEnum e)
//    {
////    const _$FixTypeEnumEnumMap = <FixTypeEnum, dynamic>{
////        FixTypeEnum.twoD: '2d',
////        FixTypeEnum.threeD: '3d',
////        FixTypeEnum.dgps: 'dgps',
////        FixTypeEnum.none: 'none',
////        FixTypeEnum.pps: 'pps'
////    };
//        def val = new StringBuilder("<${e.name}, dynamic>{")
//        e.enumNames.eachWithIndex { String name, int i ->
//            if (i>0) val << ','
//            val << '\n' << tab() << e.name << '.' << name << ': \'' << gen.enumValueFunction.apply(e.enumValues[i]) << '\''
//        }
//        val << '\n' << '}'
//        //MProperty p = new MProperty(name:DartJsonEmitter.enumMapName(e), genIgnore:true, const:true, cardinality: MCardinality.MAP, type:'dynamic', attr:['keyType': e], val:val.toString())
//        MProperty p = new MProperty(name:DartJsonEmitter.enumMapName(e, false), genIgnore:true, const:true, cardinality: MCardinality.MAP, type:'dynamic', attr:['keyType': e], val:val.toString())
//        support.addField(p)
//    }

}
