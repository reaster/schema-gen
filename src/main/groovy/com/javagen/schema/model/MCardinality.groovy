package com.javagen.schema.model

enum MCardinality {
    REQUIRED, //alias for ONE-TO-ONE relationship
    MAP,
    LINKEDMAP,
    SET,
    LIST,
    ARRAY,
    OPTIONAL;
    boolean isContainer() { CONTAINERS.contains(this) }
    static EnumSet<MCardinality> CONTAINERS = EnumSet.of(MAP,LINKEDMAP,SET,LIST,ARRAY)
}
