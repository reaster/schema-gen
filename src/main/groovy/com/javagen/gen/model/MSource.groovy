package com.javagen.gen.model

/**
 * Used by MModule and MClass to allow nested types.
 */
trait MSource
{
    File sourceFile
    def parent //MModule or MClass
    List<MClass> classes = []
    private Imports imports = new Imports(this)

    abstract String nestedAttr(String key)

    boolean isSource() { sourceFile != null }

    def getImports() { imports }

    def addClass(c) {
        if (c) {
            classes << c
            c.parent = this
        }
    }
    MClass lookupClass(String name) { classes.find { name == it.name } }

    /**
     * passes imports down to base classes
     */
    List<String> gatherSourceImports()
    {
        List<String> results = []
        if (sourceFile) {
            Set<String> set = [] as Set
            classes.each { MClass c ->
                c.imports.list.each {
                    set << it
                }
            }
            results = set.sort().collect()
        }
        results
    }
    /**
     * gathers imports from child classes and modules
     */
    static class Imports
    {
        Set<String> list = [] as Set
        def owner
        Imports(owner) { this.owner=owner }
        def leftShift(item) {
            if ((owner.parent instanceof MModule)) {
                list << item
            } else {
                owner.parent.imports << item
            }
        }
        boolean isEmpty() { list.isEmpty() }
        def each(Closure c) { list.each(c) }
    }
}