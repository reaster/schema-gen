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

import static com.javagen.schema.model.MMethod.IncludeProperties.allProperties
import static com.javagen.schema.model.MMethod.IncludeProperties.finalProperties
import static com.javagen.schema.model.MMethod.Stereotype.*

/**
 * Replaces broken Flutter json_serializable library code generator (until it's fixed), generating
 * the toJson and fromJson implementation functions, and support code.
 *
 * @author Richard Easterling
 */
class DartJsonEmitter extends CodeEmitter
{

    private static String toJsonMethodName(MClass c) { "_\$${c.name}ToJson" }
    private static String fromJsonMethodName(MClass c) { "_\$${c.name}FromJson" }
    private static String enumMapName(MEnum c, boolean underscore=true) { "${underscore ? '_' : ''}\$${c.name}EnumMap" }

    MModule impl = new MModule(name:'src')
    boolean genEnumDecode = false

    DartJsonEmitter()
    {
         if ( ! MTypeRegistry.isInitialized() )
            new DartTypeRegistry()
    }

    @Override
    def visit(MModule m) {
        //println "MModule: ${m.name}"
        if (m.isRoot()) {
            m.child(impl)
            impl.sourceFile = DartUtil.toGeneratedSourceFileName(m.sourceFile)
            impl.partOf = m.sourceFile.name
        }
        m.classes.each {
            if (!it.ignore)
                visit(it)
        }
        m.children.values().each { //visit submodules
            visit(it)
        }

        if (m.isRoot() && genEnumDecode) {
            genEnumDecodeMethod()
            genEnumDecodeNullableMethod()
        }
    }

    @Override
    def visit(MClass c)
    {
        c.methods.each {
            visit(it)
        }
        c.classes.each {
            visit(it)
        }
    }

    @Override
    def visit(MEnum e)
    {
        genEnumTable(e)
        genEnumDecode = true
    }

    @Override
    def visit(MField f) {
    }

    @Override
    def visit(MProperty p)
    {
    }

    @Override
    def visit(MReference r) {
        visit( (MProperty)r )
    }

    @Override
    def visit(MMethod m)
    {
        if (!m.stereotype || m.parent.isInterface())
            return
        switch (m.stereotype) {
            case toJson:
                genToJsonMethod(m)
                genFromJsonMethod(m)
                break
            case fromJson: //never hit this cus is constructor?
                break

        }
    }

    private genToJsonMethod(MMethod m)
    {
        //Map<String, dynamic> toJson() => _$GpxToJson(this);
        //Map<String, dynamic> _$GpxToJson(Gpx instance) => <String, dynamic>{
        MMethod m2 = new MMethod(name:toJsonMethodName(m.parent))
        def stringType = MType.lookupType('String')
        m2.type = new MBind(type:MType.lookupType('dynamic'), cardinality: MCardinality.MAP, name:'json')
        m2.type.attr << ['keyType':stringType]
        m2.params = [new MBind(type:m.parent, name:'instance')]
        m2.attr << ['class':m.parent]
        if (gen.includeIfNull) {
            m2.setExpr(this.&toJsonMethodBody)
        } else {
            m2.setBody(this.&toJsonMethodBody)
        }
        impl.addMethod(m2)
    }

    private boolean isMapWrapper(MField f)
    {
        MClass targetClass = f.parent instanceof MClass ? f.parent : null
        f.cardinality == MCardinality.MAP && targetClass?.fields.size() == 1
    }
    private boolean isMapWrapper(MClass c)
    {
        if (c.name=='Extensions')
            println "Extensions"
        if (c.fields.size() != 1)
            return false
        MField f = c.fields.values()[0]
        f.cardinality == MCardinality.MAP
    }

    private def toJsonMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        MClass targetClass = m.attr['class']
        boolean isWrapper = isMapWrapper(targetClass)
        if (gen.includeIfNull) {
            toJsonMethodBodyIncludeNulls(targetClass, isWrapper, m, v, hasSuper)
        } else {
            toJsonMethodBodyExcludeNulls(targetClass, isWrapper, m, v, hasSuper)
        }
    }

    private def toJsonMethodBodyExcludeNulls(MClass targetClass, boolean isWrapper, MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        /*
        //with include_if_null: false
        Map<String, dynamic> _$GpxToJson(Gpx instance) {
          final val = <String, dynamic>{};
          void addNonNullValue(String key, dynamic value) { if (value != null) { val[key] = value; } }
          addNonNullValue('creator', instance.creator);
          addNonNullValue('extensions', instance.extensions);
          return val;
        };
         */
        if (isWrapper) {
            v.out << '\n' << v.tabs << 'return instance.' << targetClass.fields.values()[0].name << ';'
        } else {
            //v.next()
            v.out << '\n' << v.tabs << 'final val = <String, dynamic>{};'
            v.out << '\n' << v.tabs << 'void addNonNullValue(String key, dynamic value) { if (value != null) { val[key] = value; } }'
            for(MField f in targetClass.fields.values()) {
                if (!f.isStatic() && !f.isGenIgnore()) {
                    if (f.isArray() || f.isList() || f.isSet()) {
                        def toJsonMethod = toJsonMethodName(f.type)
                        v.out << '\n' << v.tabs << 'addNonNullValue(\'' << f.name << '\', instance.' << f.name << '==null ? null : instance.' << f.name << '.map(' << toJsonMethod << ').toList());'
                    } else if (f.isMap()) {
                        v.out << '\n' << v.tabs << 'addNonNullValue(\'' << f.name << '\': instance.' << f.name << ');'
                    } else if (f.type instanceof MEnum) {
                        def toJsonMapName = enumMapName(f.type)
                        v.out << '\n' << v.tabs << 'addNonNullValue(\'' << f.name << '\', ' << toJsonMapName << '[instance.' << f.name << ']);'
                    } else if (f.isReference()) {
                        def toJsonMethod = toJsonMethodName(f.type)
                        v.out << '\n' << v.tabs << 'addNonNullValue(\'' << f.name << '\', instance.' << f.name << '==null ? null : ' << toJsonMethod << '(instance.' << f.name << '));'
                    } else if (f.type.name == 'DateTime') {
                        v.out << '\n' << v.tabs << 'addNonNullValue(\'' << f.name << '\', instance.' << f.name << '?.toIso8601String());'
                    } else if (f.type.name == 'Uri') {
                        v.out << '\n' << v.tabs << 'addNonNullValue(\'' << f.name << '\', instance.' << f.name << '?.toString());'
                    } else {
                        v.out << '\n' << v.tabs << 'addNonNullValue(\'' << f.name << '\', instance.' << f.name << ');'
                    }
                }
            }
            //v.previous()
            v.out << '\n' << v.tabs << 'return val;'
        }
    }
    private def toJsonMethodBodyIncludeNulls(MClass targetClass, boolean isWrapper, MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        /*
        //with include_if_null: true
        Map<String, dynamic> _$GpxToJson(Gpx instance) => <String, dynamic>{
          'creator': instance.creator,
          'fix': _$FixTypeEnumEnumMap[instance.fix],
          'metadata': _$MetadataToJson(instance.metadata),
          'wpts': instance.wpts==null ? null : instance.wpts.map(_$WptToJson).toList(),
          'extensions': instance.extensions==null ? null : _$ExtensionsToJson(instance.extensions)
        };
         */
        if (!isWrapper) {
            v.out << '<String, dynamic>{'
        }
        v.next()
        int i = 0
        for(MField f in targetClass.fields.values()) {
            if (!f.isStatic() && !f.isGenIgnore()) {
                if (i>0) {
                    v.out << ','
                }
                if (f.isArray() || f.isList() || f.isSet()) {
                    def toJsonMethod = toJsonMethodName(f.type)
                    v.out << '\n' << v.tabs << '\'' << f.name << '\': instance.' << f.name << '==null ? null : instance.' << f.name << '.map(' << toJsonMethod << ').toList()'
                } else if (f.isMap()) {
                    if (isMapWrapper(f)) {
                        v.out << '\n' << v.tabs << 'instance.' << f.name
                    } else {
                        v.out << '\n' << v.tabs << '\'' << f.name << '\': instance.' << f.name
                    }
                } else if (f.type instanceof MEnum) {
                    def toJsonMapName = enumMapName(f.type)
                    v.out << '\n' << v.tabs << '\'' << f.name << '\': ' << toJsonMapName << '[instance.' << f.name << ']'
                } else if (f.isReference()) {
                    def toJsonMethod = toJsonMethodName(f.type)
                    v.out << '\n' << v.tabs << '\'' << f.name << '\': instance.' << f.name << '==null ? null : ' << toJsonMethod << '(instance.' << f.name << ')'
                } else if (f.type.name == 'DateTime') {
                    v.out << '\n' << v.tabs << '\'' << f.name << '\': instance.' << f.name << '?.toIso8601String()'
                } else if (f.type.name == 'Uri') {
                    v.out << '\n' << v.tabs << '\'' << f.name << '\': instance.' << f.name << '?.toString()'
                } else {
                    v.out << '\n' << v.tabs << '\'' << f.name << '\': instance.' << f.name
                }
                i++
            }
        }
        v.previous()
        if (!isWrapper) {
            v.out << '\n' << v.tabs << '}'
        }
    }

    private genFromJsonMethod(MMethod m)
    {
        //factory Metadata.fromJson(Map<String, dynamic> json) => _$MetadataFromJson(json);
        //Metadata _$MetadataFromJson(Map<String, dynamic> json) {
        MMethod m2 = new MMethod(name:fromJsonMethodName(m.parent))
        m2.type = m.parent
        def stringType = MType.lookupType('String')
        m2.params = [new MBind(type: MType.lookupType('dynamic'), cardinality: MCardinality.MAP, attr:['keyType':stringType], name: 'json')]
        m2.attr << ['class':m.parent]
        m2.setBody(this.&fromJsonMethodBody)
        impl.addMethod(m2)
    }

    private def fromJsonMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        /*
        return Gpx(
          creator: json['creator'] as String,
          sat: json['sat'] as int,
          hdop: (json['hdop'] as num)?.toDouble(),
          time: json['time'] == null ? null : DateTime.parse(json['time'] as String),
          fix: _$enumDecodeNullable(_$FixTypeEnumEnumMap, json['fix']),
          metadata: json['metadata'] == null
              ? null
              : Metadata.fromJson(json['metadata'] as Map<String, dynamic>),
          wpts: (json['wpts'] as List)
              ?.map( (e) => e == null ? null : Wpt.fromJson(e as Map<String, dynamic>) )
              ?.toList(),
          extensions: json['extensions'] == null
              ? null
              : Extensions.fromJson(json['extensions'] as Map<String, dynamic>),
         );
         */
        MClass targetClass = m.attr['class']
        boolean isWrapper = isMapWrapper(targetClass)
        v.out << '\n' << v.tabs << 'return ' << targetClass.name << '('
        v.next()
        int i = 0
        for(MField f in targetClass.fields.values()) {
            if (!f.isStatic() && !f.isFinal() && !f.isGenIgnore()) {
                if (i>0) {
                    v.out << ','
                }
                if (f.isArray() || f.isList() || f.isSet()) {
                    //def toJsonMethod = toJsonMethodName(f.type)
                    v.out << '\n' << v.tabs << f.name << ': (json[\'' << f.name << '\'] as List)'
                    v.next()
                    v.out << '\n' << v.tabs << '?.map( (e) => e == null ? null : ' << f.type.name << '.fromJson(e as Map<String, dynamic>) )?.toList()'
                    v.previous()
                } else if (f.isMap()) {
                    if (isMapWrapper(f)) {
                        v.out << '\n' << v.tabs << f.name << ': json'
                    } else {
                       // Map<String,String> convertedMap = new Map.fromIterable(json.entries,
                       //         key: (entry) => entry.key,
                       //         value: (entry) => entry.value as String);
                        v.out << '\n' << v.tabs << f.name << ': (json[\'' << f.name << '\'] as Map)'
                        v.next()
                        v.out << '\n' << v.tabs << '?.map( (e) => e == null ? null : ' << f.type.name << '.fromJson(e as Map<String, dynamic>) )?.toList()'
                        v.previous()
                    }
                } else if (f.type instanceof MEnum) {
                    def toJsonMapName = enumMapName(f.type)
                    v.out << '\n' << v.tabs << f.name << ':  _$enumDecodeNullable(' << toJsonMapName << ', json[\'' << f.name << '\'])'
                    //fix: _$enumDecodeNullable(_$FixTypeEnumEnumMap, json['fix']),
                } else if (f.isReference()) {
                    //def fromJsonMethod = fromJsonMethodName(f.type)
                    v.out << '\n' << v.tabs << f.name << ': json[\'' << f.name << '\'] == null'
                    v.next()
                    v.out << '\n' << v.tabs << '? null'
                    v.out << '\n' << v.tabs << ': ' << f.type.name << '.fromJson(json[\'' << f.name << '\'] as Map<String, dynamic>)'
                    v.previous()
                } else if (f.type.name in ['String', 'int', 'bool']) {
                    v.out << '\n' << v.tabs << f.name << ': json[\'' << f.name << '\'] as ' << f.type.name
                } else if (f.type.name == 'double') {
                    v.out << '\n' << v.tabs << f.name << ': (json[\'' << f.name << '\'] as num)?.toDouble()'
                } else if (f.type.name == 'DateTime') {
                    v.out << '\n' << v.tabs << f.name << ': json[\'' << f.name << '\'] == null ? null : DateTime.parse(json[\'' << f.name << '\'] as String)'
                } else if (f.type.name == 'Uri') {
                    v.out << '\n' << v.tabs << f.name << ': json[\'' << f.name << '\'] == null ? null : Uri.parse(json[\'' << f.name << '\'] as String)'
                } else {
                    v.out << '\n' << v.tabs << f.name << ': json[\'' << f.name << '\'] as ' << f.type.name
                }
                i++
            }
        }
        v.previous()
        v.out << '\n' << v.tabs << ');'
    }

    private genEnumTable(MEnum e)
    {
//    const _$FixTypeEnumEnumMap = <FixTypeEnum, dynamic>{
//        FixTypeEnum.twoD: '2d',
//        FixTypeEnum.threeD: '3d',
//        FixTypeEnum.dgps: 'dgps',
//        FixTypeEnum.none: 'none',
//        FixTypeEnum.pps: 'pps'
//    };
        def val = new StringBuilder("<${e.name}, dynamic>{")
        e.enumNames.eachWithIndex { String name, int i ->
            if (i>0) val << ','
            val << '\n' << tab() << e.name << '.' << name << ': \'' << e.enumValues[i] << '\''
        }
        val << '\n' << '}'
        MProperty p = new MProperty(name:enumMapName(e, false), genIgnore:true, const:true, cardinality: MCardinality.MAP, type:'dynamic', attr:['keyType': e], val:val.toString())
        impl.addField(p)
    }

    private genEnumDecodeMethod()
    {
        //T _$enumDecode<T>(Map<T, dynamic> enumValues, dynamic source) {
        MType generic = new MType(name:'T', generic:true)
        MType dynamic = MType.lookupType('dynamic');
        MMethod m2 = new MMethod(name:'_$enumDecode', type:generic)
        m2.params = [new MBind(name: 'enumValues', type:dynamic, cardinality: MCardinality.MAP, attr:['keyType': generic]), new MBind(name: 'source', type:dynamic)]
        m2.setBody(this.&enumDecodeMethodBody)
        impl.addMethod(m2)
    }

    private def enumDecodeMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        /*
        T _$enumDecode<T>(Map<T, dynamic> enumValues, dynamic source) {
            if (source == null) {
                throw ArgumentError('A value must be provided. Supported values: ${enumValues.values.join(', ')}');

            }
            return enumValues.entries
                    .singleWhere((e) => e.value == source,
                    orElse: () => throw ArgumentError('`$source` is not one of the supported values: ${enumValues.values.join(', ')}'))
            .key;
        }
        */
        v.out << '\n' << v.tabs << 'if (source == null) {'
        v.out << '\n' << v.tabs <<  v.tab() << 'throw ArgumentError(\'A value must be provided. Supported values: ${enumValues.values.join(\', \')}\');'
        v.out << '\n' << v.tabs << '}'
        v.out << '\n' << v.tabs << 'return enumValues.entries'
        v.out << '\n' << v.tabs <<  v.tab() << '.singleWhere((e) => e.value == source,'
        v.out << '\n' << v.tabs <<  v.tab() << 'orElse: () => throw ArgumentError(\'`$source` is not one of the supported values: ${enumValues.values.join(\', \')}\'))'
        v.out << '\n' << v.tabs << '.key;'
    }

    private genEnumDecodeNullableMethod()
    {
        //T _$enumDecodeNullable<T>(Map<T, dynamic> enumValues, dynamic source) {
        MType generic = new MType(name:'T', generic:true)
        MType dynamic = MType.lookupType('dynamic');
        MMethod m2 = new MMethod(name:'_$enumDecodeNullable', type:generic)
        m2.params = [new MBind(name: 'enumValues', type:dynamic, cardinality: MCardinality.MAP, attr:['keyType': generic]), new MBind(name: 'source', type:dynamic)]
        m2.setBody(this.&enumDecodeNullableMethodBody)
        impl.addMethod(m2)
    }

    private def enumDecodeNullableMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        /*
        T _$enumDecodeNullable<T>(Map<T, dynamic> enumValues, dynamic source) {
            if (source == null) {
                return null;
            }
            return _$enumDecode<T>(enumValues, source);
        }
        */
        v.out << '\n' << v.tabs << 'if (source == null) { return null; }'
        v.out << '\n' << v.tabs << 'return _$enumDecode<T>(enumValues, source);'
    }

}
