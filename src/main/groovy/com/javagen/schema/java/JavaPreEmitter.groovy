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

import com.javagen.schema.common.CodeEmitter
import com.javagen.schema.model.MBind
import com.javagen.schema.model.MClass
import com.javagen.schema.model.MEnum
import com.javagen.schema.model.MField
import com.javagen.schema.model.MMethod
import com.javagen.schema.model.MModule
import com.javagen.schema.model.MProperty
import com.javagen.schema.model.MReference
import com.javagen.schema.model.MType
import com.javagen.schema.model.MTypeRegistry

import static com.javagen.schema.model.MCardinality.LIST
import static com.javagen.schema.model.MCardinality.SET
import static com.javagen.schema.model.MMethod.IncludeProperties.allProperties
import static com.javagen.schema.model.MMethod.IncludeProperties.finalProperties
import static com.javagen.schema.model.MMethod.Stereotype.adder
import static com.javagen.schema.model.MMethod.Stereotype.constructor
import static com.javagen.schema.model.MMethod.Stereotype.equals
import static com.javagen.schema.model.MMethod.Stereotype.getter
import static com.javagen.schema.model.MMethod.Stereotype.hash
import static com.javagen.schema.model.MMethod.Stereotype.setter
import static com.javagen.schema.model.MMethod.Stereotype.toString
import static com.javagen.schema.model.MMethod.Stereotype.toStringBuilder
import static com.javagen.schema.common.GlobalFunctionsUtil.*

/**
 * Generates addMethod signatures and default implementations from stereotypes (getters, setters, hash, equals, toString).
 *
 * @author Richard Easterling
 */
class JavaPreEmitter extends CodeEmitter
{
    EnumSet<MMethod.Stereotype> CLASS_METHODS = EnumSet.noneOf(MMethod.Stereotype) //EnumSet.of(equals, hash, toString, toStringBuilder)
    EnumSet<MMethod.Stereotype> defaultMethods = EnumSet.noneOf(MMethod.Stereotype) //EnumSet.of(equals, hash, toString, toStringBuilder, getter, setter, adder)

    JavaPreEmitter()
    {
        //HACK to fix EnumSet.of() bug
        CLASS_METHODS.add(equals)
        CLASS_METHODS.add(hash)
        CLASS_METHODS.add(toString)
        CLASS_METHODS.add(toStringBuilder)
        defaultMethods.add(equals)
        defaultMethods.add(hash)
        defaultMethods.add(toString)
        defaultMethods.add(toStringBuilder)
        defaultMethods.add(getter)
        defaultMethods.add(setter)
        defaultMethods.add(adder)
        if ( ! MTypeRegistry.isInitialized() )
            new JavaTypeRegistry()
    }

    @Override
    def visit(MModule m) {
        m.classes.each {
            visit(it)
        }
        m.children.values().each { //visit submodules
            visit(it)
        }
    }

    @Override
    def visit(MClass c) {
        defaultMethods.each {
            if (CLASS_METHODS.contains(it))
                c.addMethod(new MMethod(stereotype: it))
        }
        c.methods.each {
            visit(it)
        }
        c.classes.each {
            visit(it)
        }
        //generate accessors
        c.fields.values().each {
            visit(it)
        }
    }

    @Override
    def visit(MEnum e)
    {
        e.methods.each {
            visit(it)
        }
        //generate accessors
        e.fields.values().each {
            visit(it)
        }
    }

    @Override
    def visit(MField f) {
    }

    @Override
    def visit(MProperty p)
    {
        if (p.isStatic())
            return
        //generate accessors
        EnumSet<MMethod.Stereotype> accessors = EnumSet.noneOf(MMethod.Stereotype.class)//.of(equals, hash)
        if (!p.isFinal() && defaultMethods.contains(setter))
            accessors.add(setter)
        if (p.isContainerType(EnumSet.of(LIST, SET)) && defaultMethods.contains(adder))
            accessors.add(adder)
        if (defaultMethods.contains(getter))
            accessors.add(getter)
        genAccessors(p, accessors)
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
            case constructor:
                m.name = m.parent.shortName()
                m.body = m.body ?: this.&constructorMethodBody
                switch (m.includeProperties) {
                    case finalProperties:
                        m.params = m.parent.fields.values().findAll { p -> p.isFinal() && !p.isStatic() }
                        break
                    case allProperties:
                        m.params = m.parent.fields.values().findAll { p -> !p.isStatic() }
                }
                break
            case hash:
                m.annotations << '@Override'
                m.type = 'int'
                m.name = 'hashCode'
                m.body = m.body ?: this.&hashCodeMethodBody
                break
            case equals:
                m.annotations << '@Override'
                m.type = 'boolean'
                m.name = 'equals'
                m.params = [ new MBind(type:MType.lookupType('Object'), name:'o') ]
                m.body = m.body ?: this.&equalsMethodBody
                break
            case toString:
                m.annotations << '@Override'
                m.type = 'String'
                m.name = 'toString'
                m.body = m.body ?: this.&toStringMethodBody
                break
            case toStringBuilder:
                if (m.parent.hasSuper())
                    m.annotations << '@Override'
                m.name = 'toString'
                m.scope = 'protected'
                m.params = [ new MBind(type:MType.lookupType('StringBuilder'), name:'sb') ]
                m.body = m.body ?: this.&toStringBuilderMethodBody
                break
        }
    }

    private def toStringBuilderMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        def fields = m.parent.fields.values().findAll{ p -> !p.isStatic() }
        if (hasSuper) {
            v.out << '\n' << v.tabs << 'super.toString(sb);'
        }
        fields.eachWithIndex { f, i ->
            v.out << '\n' << v.tabs << "sb.append(\"${(i>0 || hasSuper ? ', ' : '')}${f.name}=\").append(${f.name})" << ';'
        }
    }

    private def toStringMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        def name = m.parent.shortName()
        v.out << '\n' << v.tabs << "StringBuilder sb = new StringBuilder(\"${name}[\");"
        v.out << '\n' << v.tabs << 'toString(sb);'
        v.out << '\n' << v.tabs << 'return sb.append(\"]\").toString();'
    }

    private def toStringMethodBodyStandAlone(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        def name = m.parent.shortName()
        def fields = m.parent.fields.values().findAll{ p -> !p.isStatic() }
        v.out << '\n' << v.tabs << "StringBuilder sb = new StringBuilder(\"${name}[\");"
        fields.eachWithIndex { f, i ->
            v.out << '\n' << v.tabs << "sb.append(\"${(i>0 ? ', ' : '')}${f.name}=\").append(${f.name})" << ';'
        }
        v.out << '\n' << v.tabs << 'return sb.append(\"]\").toString();'
    }

    private def constructorMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        if ( ! m.parent.isEnum() )
            v.out << '\n' << v.tabs << 'super();'
        for(param in m.params) {
            v.out << '\n' << v.tabs << 'this.' << param.name << ' = ' << param.name << ';'
        }
    }


    private def genAccessors(MProperty p, EnumSet<MMethod.Stereotype> accessors)
    {
        def upCaseName = upperCase(p.name)
        accessors.each {
            switch (it) {
                case setter:
                    def method = p.methods[setter]
                    def setterName = method?.name
                    if (!method) {
                        MBind param = new MBind(name: p.name, type: p.type, cardinality: p.cardinality)
                        Closure methodBody = p.setterBody ?: JavaPreEmitter.&setterMethodBody
                        setterName = 'set' + upCaseName
                        method = new MMethod(name: setterName, params: [param], scope: 'public', body: methodBody, stereotype: setter)
                        method.refs['property'] = p
                        p.methods[setter] = method
                    }
                    if (p.parent.hasMethod(setterName)) {
                        println "WARNING: method already defined - ingoring property setter: ${setterName} for ${p}"
                    } else {
                        p.parent.addMethod(method)
                    }
                    break
                case adder:
                    def method = p.methods[adder]
                    def adderName = method?.name
                    if (!method) {
                        def singular = p.attr['singular']
                        adderName = 'add' + (singular ? upperCase(singular.toString()) : upCaseName)
                        MBind param = new MBind(name: singular ?: p.name, type: p.type)
                        Closure methodBody = JavaPreEmitter.&adderMethodBody
                        method = new MMethod(name: adderName, params: [param], scope: 'public', body: methodBody, stereotype: adder)
                        method.refs['property'] = p
                        p.methods[adder] = method
                    }
                    if (p.parent.hasMethod(adderName)) {
                        println "WARNING: method already defined - ingoring property adder: ${adderName} for ${p}"
                    } else {
                        p.parent.addMethod(method)
                    }
                    break
                case getter:
                    def method = p.methods[getter]
                    def getterName = method?.name
                    if (!method) {
                        getterName = JavaTypeRegistry.isBoolean(p.type) && !p.isContainerType() ? 'is' + upCaseName : 'get' + upCaseName
                        MBind type = new MBind(type: p.type, cardinality: p.cardinality)
                        Closure methodBody = p.getterBody ?: JavaPreEmitter.&getterMethodBody
                        method = new MMethod(name: getterName, type: type, scope: 'public', body: methodBody, stereotype: getter)
                        method.refs['property'] = p
                        p.methods[getter] = method
                    }
                    if (p.parent.hasMethod(getterName)) {
                        println "WARNING: method already defined - ingoring property getter: ${getterName} for ${p}"
                    } else {
                        p.parent.addMethod(method)
                    }
                    break
                default:
                    def method = p.methods[it]
                    def methodName = method?.name
                    if (p.parent.hasMethod(methodName)) {
                        println "WARNING: method already defined - ingoring property ${it}: ${methodName} for ${p}"
                    } else {
                        p.parent.addMethod(method)
                    }
            }
        }
        p.methods.keySet().each {
            if (!accessors.contains(it)) {
                def method = p.methods[it]
                def methodName = method?.name
                if (p.parent.hasMethod(methodName)) {
                    println "WARNING: method already defined - ingoring property ${it}: ${methodName} for ${p}"
                } else {
                    p.parent.addMethod(method)
                }
            }
        }
    }

    static def getterMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        MProperty prop = (MProperty)m.refs['property']
        def propName = prop.name
        v.out << '\n' << v.tabs << 'return ' << propName << ';'
    }
    static def setterMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        MProperty prop = (MProperty)m.refs['property']
        def propName = prop.name
        v.out << '\n' << v.tabs << 'this.' << propName << ' = ' << propName << ';'
    }
    static def putterMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        def prop = m.refs['property']
        def propName = prop.name
        assert m.params.size() == 2
        v.out << '\n' << v.tabs << 'this.' << propName << '.put(' << m.params[0].name << ', ' << m.params[1].name << ');'
    }
    static def adderMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
        MProperty prop = (MProperty)m.refs['property']
        def propName = prop.name
        def paramName = m.params[0].name
        v.out << '\n' << v.tabs << 'if (' << 'this.' << propName << ' == null)'
        v.next()
        v.out << '\n' << v.tabs << 'this.' << propName << ' = new ' << JavaTypeRegistry.containerImplementation(prop.cardinality) << '<>();'
        v.previous()
        v.out << '\n' << v.tabs << 'this.' << propName << '.add(' << paramName << ');'
    }


    private def equalsMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//		@Override
//		public boolean equals(Object o)
//		{
//			if (this == o) return true;
//			if (o == null || getClass() != o.getClass()) return false;
//			Hours hours = (Hours) o;
//			if (prefix != null ? !prefix.equals(hours.prefix) : hours.prefix != null) return false;
//			if (days != null ? !days.equals(hours.days) : hours.days != null) return false;
//			return note != null ? note.equals(hours.note) : hours.note == null;
//		}
        def fields = m.parent.fields.values().findAll{ p -> !p.isStatic() }
        def type = m.parent.shortName()
        if (hasSuper) {
            v.out << '\n' << v.tabs << 'if (!super.equals(o)) return false;'
        } else {
            v.out << '\n' << v.tabs << 'if (this == o) return true;'
        }
        v.out << '\n' << v.tabs << 'if (o == null || getClass() != o.getClass()) return false;'
        v.out << '\n' << v.tabs << "${type} other = (${type})o;"
        fields.each { f ->
            if (f.type.isPrimitive()) {
                switch (f.type.name) {
                    case 'float':
                        v.out << '\n' << v.tabs << "if (Float.compare(other.${f.name}, ${f.name}) != 0) return false;"
                        break
                    case 'double':
                        v.out << '\n' << v.tabs << "if (Double.compare(other.${f.name}, ${f.name}) != 0) return false;"
                        break
                    default:
                        v.out << '\n' << v.tabs << "if (${f.name} != other.${f.name}) return false;"
                }
            } else if (f.isArray()) {
                v.out << '\n' << v.tabs << "if (!Arrays.equals(${f.name}, other.${f.name})) return false;"
            } else {
                v.out << '\n' << v.tabs << "if (${f.name} != null ? !${f.name}.equals(other.${f.name}) : other.${f.name} != null) return false;"
            }
        }
        v.out << '\n' << v.tabs << 'return true;'
    }

    private def hashCodeMethodBody(MMethod m, CodeEmitter v, boolean hasSuper=false)
    {
//		@Override
//		public int hashCode()
//		{
//			int result = prefix != null ? prefix.hashCode() : 0;
//			result = 31 * result + (days != null ? days.hashCode() : 0);
//			return result;
//		}
        v.out << '\n' << v.tabs << 'int result = '
        v.out << (hasSuper ? 'super.hashCode()' : '1')
        v.out << ';'
        def fields = m.parent.fields.values().findAll{ p -> !p.isStatic() }
        fields.each { f ->
            if (f.type.isPrimitive()) {
                switch (f.type.name) {
                    case 'boolean':
                        v.out << '\n' << v.tabs << "result = 31 * result + (${f.name} ? 1 : 0);"
                        break
                    case ['byte','char','short']:
                        v.out << '\n' << v.tabs << "result = 31 * result + (int)${f.name};"
                        break
                    case 'long':
                        v.out << '\n' << v.tabs << "result = 31 * result + (int) (${f.name} ^ (${f.name} >>> 32));"
                        break
                    case 'float':
                        v.out << '\n' << v.tabs << "result = 31 * result + (${f.name} != +0.0f ? Float.floatToIntBits(${f.name}) : 0);"
                        break
                    case 'double':
                        v.out << '\n' << v.tabs << "final long ${f.name}Temp = Double.doubleToLongBits(${f.name});"
                        v.out << '\n' << v.tabs << "result = 31 * result + (int) (${f.name}Temp ^ (${f.name}Temp >>> 32));"
                        break
                    default:
                        v.out << '\n' << v.tabs << "result = 31 * result + ${f.name};"
                }
            } else if (f.isArray()) {
                v.out << '\n' << v.tabs << "result = 31 * result + Arrays.hashCode(${f.name});"
            } else {
                v.out << '\n' << v.tabs << "result = 31 * result + (${f.name} != null ? ${f.name}.hashCode() : 0);"
            }
        }
        v.out << '\n' << v.tabs << 'return result;'
    }

}
