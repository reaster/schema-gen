package com.javagen.schema.xml.node

class Restriction
{
    static enum RType {
        enumeration,
        minInclusive,
        maxInclusive,
        minExclusive,
        maxExclusive,
        pattern,
        whiteSpace,
        length,
        minLength,
        maxLength,
        fractionDigits,
        totalDigits
    }
    final RType type
    final String value
    Restriction(RType type, String value) { this.type=type; this.value=value }
    Restriction(String type, String value) { this.type=RType.valueOf(type); this.value=value }
    @Override String toString() { "${type.name()}=${value}" }
}
