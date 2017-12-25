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

package com.javagen.schema.common

import groovy.transform.CompileStatic

/**
 * Used for generating one-to-many property names. Builds on the stateless GlobalFunctionsUtil.toPlural() and
 * GlobalFunctionsUtil.toSingular() functions to add manual
 * mapping for irregular nouns and caching. Because the toSingular() method does a much poorer job than toPlural() at
 * producing correct English, caching improves the results when toPlural() is called first (which is generaly the case
 * in this code base).
 *
 * @author Richard Easterling
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
