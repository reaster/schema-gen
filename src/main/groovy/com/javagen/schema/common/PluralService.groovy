package com.javagen.schema.common

import groovy.transform.CompileStatic

/**
 * Adds manual mapping for irregular nouns and caching.
 */
@CompileStatic
class PluralService
{
    final Map<String,String> singularToPluralMappings
    final Map<String,String> pluralToSingularMappings

    PluralService(Map<String,String> singularToPluralMappings)
    {
        this.singularToPluralMappings = singularToPluralMappings ?: [:] as Map<String,String>
        this.pluralToSingularMappings = this.singularToPluralMappings.collectEntries { e-> [e.value,e.key] } as Map<String,String>
    }
    PluralService() { this([:]) }

    String toPlural(String singular)
    {
        if (!singular)
            return singular
        String plural = singularToPluralMappings[singular]
        if (plural) {
            return plural
        } else {
            plural = GlobalFunctionsUtil.toPlural(singular)
            //cache results, cus it's harder to do plural -> singular
            singularToPluralMappings[singular] = plural
            pluralToSingularMappings[plural] = singular
            return plural
        }
    }

    String toSingular(String plural)
    {
        if (!plural)
            return plural
        final String singular = pluralToSingularMappings[plural]
        if (singular) {
            return singular
        } else {
            return GlobalFunctionsUtil.toSingular(plural)
        }
    }

}
