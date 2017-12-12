package com.javagen.schema.xml.node
/**
 * A TextOnlyType describes an atomic type of an attribute or single value element and is typically employed to apply
 * extra restrictions to a basic xml type. In the simplest case, TextOnlyTypes are mapped directly to MProperty, but
 * depending on the restrictions complete mapping may require data validation rules (for example Java @Min/@Max/@RegExpr
 * annotations), enumeration classes, cardinality generation (when defined in a List element or defined as a multi-value
 * basic type like
 *
 * types
 */
//@ToString(includeSuper=true,includePackage=false)
class TextOnlyType extends Type
{
    java.util.List<Restriction> restrictions = []
    EnumSet<Restriction.RType> restrictionSet() {
        EnumSet<Restriction.RType> set = EnumSet.noneOf(Restriction.RType);
        restrictions.each{ set.add(it.type) };
        set
    }
}
