package com.javagen.gen.model

class MEnum extends MClass
{
    def enumNames = []
    def enumValues = []
    def enumDefault
    @Override boolean isEnum() { true }
}
