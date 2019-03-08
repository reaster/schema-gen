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

import static com.javagen.schema.common.GlobalFunctionsUtil.upperCase
import static com.javagen.schema.model.MCardinality.LIST
import static com.javagen.schema.model.MCardinality.SET
import static com.javagen.schema.model.MMethod.IncludeProperties.allProperties
import static com.javagen.schema.model.MMethod.IncludeProperties.finalProperties
import static com.javagen.schema.model.MMethod.Stereotype.*

/**
 * Generates boilerplate methods from stereotypes (hash, equals, toJson, fromJson, toString).
 *
 * @author Richard Easterling
 */
class DartPreEmitter extends CodeEmitter
{
    DartPreEmitter()
    {
        configDefaultMethods()

        if ( ! MTypeRegistry.isInitialized() )
            new DartTypeRegistry()
    }

    @Override
    def visit(MModule m)
    {
        m.classes.each {
            visit(it)
        }
        m.children.values().each { //visit submodules
            visit(it)
        }
    }

    @Override
    def visit(MClass c)
    {
        addDefaultClassMethodSubs(c)

        c.methods.each {
            visit(it)
        }
        c.classes.each {
            visit(it)
        }
        if (c.isInterface()) {
            propertiesToAbstractAccessors(c)
        } else {
            c.fields.values().each {
                visit(it)
            }
        }
    }

    private propertiesToAbstractAccessors(MClass c)
    {
        List<MField> fields = c.fields.values().findAll{ !it.isStatic() }
        for(MField f : fields) {
            MMethod getter = new MMethod(name:f.name, refs:['property':f], stereotype: MMethod.Stereotype.getter, getter:true, abstract: true, type:f.type)
            c.addMethod(getter)
            MMethod setter = new MMethod(name:f.name, refs:['property':f], stereotype: MMethod.Stereotype.setter, setter:true, abstract: true, params:[new MBind(type:f.type,name:f.name)])
            c.addMethod(setter)
            c.fields.remove(f.name)
        }
    }

    @Override
    def visit(MEnum e)
    {
        e.methods.each {
            visit(it)
        }
        e.fields.values().each {
            visit(it)
        }
    }

    @Override
    def visit(MField f)
    {
    }

    @Override
    def visit(MProperty p)
    {
    }

    @Override
    def visit(MReference r)
    {
        visit( (MProperty)r )
    }

    @Override
    def visit(MMethod m)
    {
        if (!m.stereotype || m.parent.isInterface())
            return
        MClass c = m.parent instanceof MClass ? m.parent : null
        switch (m.stereotype) {
            case constructor:
                m.name = c.shortName()
                //m.body = m.body ?: this.&constructorMethodBody
                switch (m.includeProperties) {
                    case finalProperties:
                        m.params = c.inheritedFields().values().findAll { p -> p.isFinal() && !p.isStatic() && !p.isGenIgnore() }
                        break
                    case allProperties:
                        m.params = c.inheritedFields().values().findAll { p -> !p.isFinal() && !p.isStatic() && !p.isGenIgnore() }
                }
                break
            case toJson:
                if (gen.includeJsonSupport) {
                    //Map<String, dynamic> toJson() => _$UserToJson(this);
                    def stringType = MType.lookupType('String')
                    m.type = new MBind(type:MType.lookupType('dynamic'), cardinality: MCardinality.MAP, name:'json')
                    m.type.attr << ['keyType':stringType]
                    m.name = 'toJson'
                    m.setExpr("_\$${m.parent.name}ToJson(this)")
                }
                break
            case fromJson:
                if (gen.includeJsonSupport) {
                    //factory User.fromJson(Map<String, dynamic> json) => _$UserFromJson(json);
                    m.factory = true
                    m.stereotype = MMethod.Stereotype.constructor
                    m.name = "${m.parent.name}.fromJson"
                    def stringType = MType.lookupType('String')
                    m.params = [ new MBind(type:MType.lookupType('dynamic'), cardinality: MCardinality.MAP, attr:['keyType':stringType], name:'json') ]
                    m.setExpr("_\$${m.parent.name}FromJson(json)")
                }
                break
            case hash:
                m.annotations << '@override'
                m.type = 'int'
                m.name = 'hashCode'
                m.getter = true
                m.setBody(this.&hashCodeMethodBody)
                break
            case equals:
                m.annotations << '@override'
                m.type = 'bool'
                m.name = '=='
                m.operator = true
                m.params = [ new MBind(type:MType.lookupType('dynamic'), name:'o') ]
                m.setExpr(this.&equalsMethodBody)
                break
            case toString:
                m.annotations << '@override'
                m.type = 'String'
                m.name = 'toString'
                m.body = m.body ?: this.&toStringMethodBody
                break
            case toStringBuilder:
                if (m.parent.hasSuper())
                    m.annotations << '@override'
                m.name = 'toString'
                m.params = [ new MBind(type:MType.lookupType('StringBuilder'), name:'sb') ]
                m.body = m.body ?: this.&toStringBuilderMethodBody
                break
            case equalsList:
                m.name = '_equalsList'
                m.type = 'bool'
                m.scope = 'private'
                def containerType = MType.lookupType('List')
                m.params = [ new MBind(type:containerType, name:'a'), new MBind(type:containerType, name:'b')  ]
                m.body = m.body ?: this.&equalsListMethodBody
                break
            case equalsMap:
                m.name = '_equalsMap'
                m.type = 'bool'
                m.scope = 'private'
                def containerType = MType.lookupType('Map')
                m.params = [ new MBind(type:containerType, name:'a'), new MBind(type:containerType, name:'b')  ]
                m.body = m.body ?: this.&equalsMapMethodBody
                break
            case equalsSet:
                m.name = '_equalsSet'
                m.type = 'bool'
                m.scope = 'private'
                def containerType = MType.lookupType('Set')
                m.params = [ new MBind(type:containerType, name:'a'), new MBind(type:containerType, name:'b')  ]
                m.body = m.body ?: this.&equalsSetMethodBody
                break
        }
    }


    private def equalsListMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        v.out << '\n' << v.tabs << 'if (identical(a, b) || (a==null && b==null)) return true;'
        v.out << '\n' << v.tabs << 'if (a==null || b==null || a.length != b.length) return false;'
        v.out << '\n' << v.tabs << 'var i = 0;'
        v.out << '\n' << v.tabs << 'return a.every((o) { return a[i++] == o; });'
    }

    private def equalsMapMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        v.out << '\n' << v.tabs << 'if (identical(a, b) || (a==null && b==null)) return true;'
        v.out << '\n' << v.tabs << 'if (a==null || b==null || a.length != b.length) return false;'
        v.out << '\n' << v.tabs << 'return a.keys.every( (key) => b.containsKey(key) && a[key] == b[key] );'
    }

    private def equalsSetMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        v.out << '\n' << v.tabs << 'if (identical(a, b) || (a==null && b==null)) return true;'
        v.out << '\n' << v.tabs << 'if (a==null || b==null || a.length != b.length) return false;'
        v.out << '\n' << v.tabs << 'return a.keys.every( (key) => b.containsKey(key) );'
    }

    private def toStringBuilderMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//        def fields = m.parent.fields.values().findAll{ p -> !p.isStatic() }
//        if (hasSuper) {
//            v.out << '\n' << v.tabs << 'super.toString(sb);'
//        }
//        fields.eachWithIndex { f, i ->
//            v.out << '\n' << v.tabs << "sb.append(\"${(i>0 || hasSuper ? ', ' : '')}${f.name}=\").append(${f.name})" << ';'
//        }
    }

    private def toStringMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//        def name = m.parent.shortName()
//        v.out << '\n' << v.tabs << "StringBuilder sb = new StringBuilder(\"${name}[\");"
//        v.out << '\n' << v.tabs << 'toString(sb);'
//        v.out << '\n' << v.tabs << 'return sb.append(\"]\").toString();'
    }

    private def toStringMethodBodyStandAlone(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//        def name = m.parent.shortName()
//        def fields = m.parent.fields.values().findAll{ p -> !p.isStatic() }
//        v.out << '\n' << v.tabs << "StringBuilder sb = new StringBuilder(\"${name}[\");"
//        fields.eachWithIndex { f, i ->
//            v.out << '\n' << v.tabs << "sb.append(\"${(i>0 ? ', ' : '')}${f.name}=\").append(${f.name})" << ';'
//        }
//        v.out << '\n' << v.tabs << 'return sb.append(\"]\").toString();'
    }

    private def constructorMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//        if ( ! m.parent.isEnum() )
//            v.out << '\n' << v.tabs << 'super();'
//        for(param in m.params) {
//            v.out << '\n' << v.tabs << 'this.' << param.name << ' = ' << param.name << ';'
//        }
    }

    private def getterMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//        MProperty prop = (MProperty)m.refs['property']
//        def propName = prop.name
//        v.out << '\n' << v.tabs << 'return ' << propName << ';'
    }
    private def setterMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//        MProperty prop = (MProperty)m.refs['property']
//        def propName = prop.name
//        v.out << '\n' << v.tabs << 'this.' << propName << ' = ' << propName << ';'
    }
    private def putterMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//        def prop = m.refs['property']
//        def propName = prop.name
//        assert m.params.size() == 2
//        v.out << '\n' << v.tabs << 'this.' << propName << '.put(' << m.params[0].name << ', ' << m.params[1].name << ');'
    }
    private def adderMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//        MProperty prop = (MProperty)m.refs['property']
//        def propName = prop.name
//        def paramName = m.params[0].name
//        v.out << '\n' << v.tabs << 'if (' << 'this.' << propName << ' == null)'
//        v.next()
//        v.out << '\n' << v.tabs << 'this.' << propName << ' = new ' << DartTypeRegistry.containerImplementation(prop.cardinality) << '<>();'
//        v.previous()
//        v.out << '\n' << v.tabs << 'this.' << propName << '.add(' << paramName << ');'
    }


    private def equalsMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//		@override
//		bool operator ==(dynamic o) =>
//			o is Gpx &&
//          super==(o) &&
//			version == o.version &&
//			creator == o.creator &&
//			metadata == o.metadata &&
//			_equalsList(wpts, o.wpts) &&
//			extensions == o.extensions;
//		}
        def fields = m.parent.fields.values().findAll{ p -> !p.isStatic() }
        def type = m.parent.shortName()
        v.next()
        v.out << '\n' << v.tabs << "o is ${type} &&"
        if (hasSuper) {
            v.out << '\n' << v.tabs << 'super==(o) &&'
        }
        int i = 0
        fields.eachWithIndex { f, index ->
            if (f.isStatic() || f.isGenIgnore())
                return
            if (i>0)
                v.out << ' &&'
            if (f.isArray() || f.isList()) {
                v.out << '\n' << v.tabs << "_equalsList(${f.name}, o.${f.name})"
            } else if (f.isMap()) {
                v.out << '\n' << v.tabs << "_equalsMap(${f.name}, o.${f.name})"
            } else if (f.isSet()) {
                v.out << '\n' << v.tabs << "_equalsSet(${f.name}, o.${f.name})"
            } else {
                v.out << '\n' << v.tabs << "${f.name} == o.${f.name}"
            }
            i++
        }
        v.previous()
    }

    private def hashCodeMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//		@override int get hashCode {
//          int result = 17;
//		    result = 37 * result + minlat?.hashCode ?? 0;
//			return result;
//		}
        v.out << '\n' << v.tabs << 'int result = '
        v.out << (hasSuper ? 'super.hashCode' : '17')
        v.out << ';'
        def fields = m.parent.fields.values().findAll{ p -> !p.isStatic() }
        fields.each { f ->
            if (f.isStatic() || f.isGenIgnore())
                return
            v.out << '\n' << v.tabs << "result = 31 * result + (${f.name}?.hashCode ?? 0);"
        }
        v.out << '\n' << v.tabs << 'return result;'
    }

    EnumSet<MMethod.Stereotype> CLASS_METHODS = EnumSet.noneOf(MMethod.Stereotype) //EnumSet.of(equals, hash, toString, toStringBuilder)
    EnumSet<MMethod.Stereotype> defaultMethods = EnumSet.noneOf(MMethod.Stereotype) //EnumSet.of(equals, hash, toString, toStringBuilder, getter, setter, adder)

    private configDefaultMethods()
    {
        //HACK to fix EnumSet.of() bug
        CLASS_METHODS.add(constructor)
        CLASS_METHODS.add(toJson)
        CLASS_METHODS.add(fromJson)
        CLASS_METHODS.add(equals)
        CLASS_METHODS.add(hash)
//        CLASS_METHODS.add(toString)
//        CLASS_METHODS.add(toStringBuilder)
        defaultMethods.add(constructor)
        defaultMethods.add(equals)
        defaultMethods.add(hash)
        defaultMethods.add(toJson)
        defaultMethods.add(fromJson)
        defaultMethods.add(toString)
        defaultMethods.add(toStringBuilder)
        defaultMethods.add(getter)
        defaultMethods.add(setter)
        defaultMethods.add(adder)
    }
    private addDefaultClassMethodSubs(MClass c)
    {
        if (c.ignore || c.isInterface())
            return
        defaultMethods.each {
            if (CLASS_METHODS.contains(it)) {
                MMethod method = new MMethod(stereotype: it)
                if (it == constructor) {
                    method.includeProperties = allProperties
                    c.addMethod(method)
                } else if (it == equals) {
                    //check for supporting methods requirements
                    if (c.fields.values().any{ p -> !p.isStatic() && p.isList() })
                        c.addMethod(new MMethod(stereotype: equalsList))
                    if (c.fields.values().any{ p -> !p.isStatic() && p.isMap() })
                        c.addMethod(new MMethod(stereotype: equalsMap))
                    if (c.fields.values().any{ p -> !p.isStatic() && p.isSet() })
                        c.addMethod(new MMethod(stereotype: equalsSet))
                    c.addMethod(method)
                } else {
                    c.addMethod(method)
                }
            }
        }
    }

}
