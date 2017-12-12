package com.javagen.schema.common

import com.javagen.schema.common.Gen
import com.javagen.schema.model.MModule

class TestGen extends Gen
{
    MModule m

    TestGen(MModule m) { this.m=m }
    TestGen() { this(null) }

    @Override MModule getModel() { return m }
}
