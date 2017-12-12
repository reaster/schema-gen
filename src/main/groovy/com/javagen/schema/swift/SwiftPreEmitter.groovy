package com.javagen.schema.swift

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

import static com.javagen.schema.model.MCardinality.ARRAY
import static com.javagen.schema.model.MCardinality.LIST
import static com.javagen.schema.model.MCardinality.MAP
import static com.javagen.schema.model.MCardinality.OPTIONAL
import static com.javagen.schema.model.MCardinality.SET
import static com.javagen.schema.model.MMethod.IncludeProperties.allProperties
import static com.javagen.schema.model.MMethod.IncludeProperties.finalProperties
import static com.javagen.schema.model.MMethod.Stereotype.constructor
import static com.javagen.schema.model.MMethod.Stereotype.equals
import static com.javagen.schema.model.MMethod.Stereotype.hash
import static com.javagen.schema.model.MMethod.Stereotype.toString

class SwiftPreEmitter extends CodeEmitter
{
    EnumSet<MMethod.Stereotype> defaultMethods = EnumSet.of(hash) //noneOf(MMethod.Stereotype.class)

    SwiftPreEmitter()
    {
        if ( ! MTypeRegistry.isInitialized() )
            new SwiftTypeRegistry()
    }

    @Override
    def visit(MModule m) {
        def classes = m.classes.findAll() //copy so items can be removed
        classes.each {
            visit(it)
        }
        m.children.values().each { //visit submodules
            visit(it)
        }
    }

    @Override
    def visit(MClass c)
    {
        defaultMethods.each {
            c.addMethod(new MMethod(stereotype: it))
        }
        def methods = c.methods.findAll() //copy so methods can be removed
        methods.each {
            visit(it)
        }
        c.classes.each {
            visit(it)
        }
    }

    @Override
    def visit(MEnum e)
    {
        e.methods.each {
            visit(it)
        }
    }

    @Override
    def visit(MField f) {
    }

    @Override
    def visit(MProperty p) {
    }

    @Override
    def visit(MReference r) {
    }

    @Override
    def visit(MMethod m)
    {
        if (!m.stereotype)
            return
        switch (m.stereotype) {
            case constructor:
                m.name = m.parent.shortName()
                switch (m.includeProperties) {
                    case finalProperties:
                        m.params = m.parent.fields.values().findAll { p -> p.isFinal() && !p.isStatic() }
                        break
                    case allProperties:
                        m.params = m.parent.fields.values().findAll { p -> !p.isStatic() }
                }
                m.body = this.&constructorMethodBody
                break
            case hash: //generate both equals and hash in an extension
                MClass c = m.parent
                MModule module = c.parentModule()
                //extension Hashable:
                MClass e = new MClass(name: c.name, extension: true)
                module.addClass(e)
                e.ignore = c.ignore
                e.attr['targetClass'] = c
                e.implements << 'Hashable'
                //equals addMethod: static func ==(rhs: Address, lhs: Address) -> Bool
                MMethod em = new MMethod(name: '==', 'static': true, type: 'Bool', stereotype: equals, body: this.&equalsMethodBody)
                em.params << new MBind(name: 'rhs', type: c.name)
                em.params << new MBind(name: 'lhs', type: c.name)
                e.addMethod(em)
                //hash: var hashValue: Int
                MField hv = new MProperty(name: 'hashValue', scope: 'public', type: 'Int', getterBody: this.&hashCodeGetterBody)
                e.addField(hv)
                c.methods.remove(m)
                break
            case equals: //generate equals in an extension
                MClass c = m.parent
                MModule module = c.parentModule()
                MClass e = new MClass(name: c.name, extension: true)
                module.addClass(e)
                e.attr['targetClass'] = c
                e.implements << 'Equatable'
                MMethod hm = new MMethod(name: '==', 'static': true, type: 'Bool', stereotype: equals, body: this.&equalsMethodBody)
                hm.params << new MBind(name: 'rhs', type: MType.lookupType(c.name))
                hm.params << new MBind(name: 'lhs', type: MType.lookupType(c.name))
                e.addMethod(hm)
                c.methods.remove(m)
                break
            case toString:
                m.annotations << '@Override'
                m.type = 'String'
                m.name = 'toString'
                break
        }
    }

    def constructorMethodBody(MMethod m, CodeEmitter v)
    {
        for(param in m.params) {
            v.out << '\n' << v.tabs << 'self.' << param.name << ' = ' << param.name
        }
    }

    def equalsMethodBody(MMethod m, CodeEmitter v)
    {
//		extension Poi: Equatable {
//			public static func ==(lhs: Poi, rhs: Poi) -> Bool {
//				guard lhs.id == rhs.id else { return false }
//				...
//				return true
//			} }
        MClass targetClass = (MClass)m.parent.attr['targetClass']
        if (!targetClass) throw new IllegalStateException("expecting addMethod.parent.attr['targetClass'] to be set")
        def fields = targetClass.fields.values().findAll{ p -> !p.isStatic() }
        fields.each { f ->
            v.out << '\n' << v.tabs << "guard lhs.${f.name} == rhs.${f.name} else { return false }"
        }
        v.out << '\n' << v.tabs << 'return true;'
    }

    def hashCodeGetterBody(MField p, CodeEmitter v)
    {
//		@Override
//		public int hashCode()
//		{
//			int result = prefix != null ? prefix.hashCode() : 0;
//			result = 31 * result + (days != null ? days.hashCode() : 0);
//			return result;
//		}
        v.out << '\n' << v.tabs << 'var result = 1'
        MClass targetClass = (MClass)p.parent.attr['targetClass']
        if (!targetClass) throw new IllegalStateException("expecting addMethod.parent.attr['targetClass'] to be set")
        def fields = targetClass.fields.values().findAll{ f -> !f.isStatic() }
        fields.each { MField f ->
            def iType = f.type.name
            switch (f.cardinality) {
                case ARRAY:
                case LIST: //phone: [Phone]
                    v.out << '\n' << v.tabs << 'result = 31 * result + ' << f.name << '.count'
                    break
                case MAP:
                    v.out << '\n' << v.tabs << 'result = 31 * result + ' << f.name << '.count'
                    break
                case SET:
                    v.out << '\n' << v.tabs << 'result = 31 * result + ' << f.name << '.count'
                    break
                case OPTIONAL:
                    v.out << '\n' << v.tabs << 'if let ' << f.name << ' = ' << f.name << ' { result = 31 * result + ' << f.name << '.hashValue }'
                    break
                default: // REQUIRED
                    v.out << '\n' << v.tabs << 'result = 31 * result + ' << f.name << '.hashValue'
            }
        }
        v.out << '\n' << v.tabs << 'return result;'
    }


}