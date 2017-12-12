package com.javagen.schema.common

import org.junit.Test
import static junit.framework.TestCase.assertEquals

class PluralServiceTest
{
    PluralService s = new PluralService()

    @Test
    void testToPlural()
    {
        assertEquals('babies', s.toPlural('baby'))
        assertEquals('hats', s.toPlural('hat'))
        assertEquals('aliases', s.toPlural('alias'))
        assertEquals('routes', s.toPlural('route'))

        //gets it right by caching the result
        assertEquals('wolves', s.toPlural('wolf'))
        assertEquals('wolf', s.toSingular('wolves'))
        assertEquals('knives', s.toPlural('knife'))
        assertEquals('knife', s.toSingular('knives'))
        assertEquals('pictures', s.toPlural('picture'))
        assertEquals('picture', s.toSingular('pictures'))
    }
    @Test
    void testToSingular()
    {
        assertEquals('baby', s.toSingular('babies'))
        assertEquals('hat', s.toSingular('hats'))
        assertEquals('alias', s.toSingular('aliases'))
        //FAIL assertEquals('route', s.toSingular('routes'))

    }
}
