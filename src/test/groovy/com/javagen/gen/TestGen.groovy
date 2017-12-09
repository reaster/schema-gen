package com.javagen.gen

import com.javagen.gen.Gen
import com.javagen.gen.model.MModule

class TestGen extends Gen
{
    MModule m

    TestGen(MModule m) { this.m=m }
    TestGen() { this(null) }

    @Override MModule getModel() { return m }
}
