package com.javagen.schema.common

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import org.junit.Test

import java.time.LocalDateTime
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

import java.time.ZoneId
import java.time.ZonedDateTime

import static junit.framework.TestCase.assertEquals
import static org.hamcrest.CoreMatchers.containsString
import static org.hamcrest.MatcherAssert.assertThat

/**
 * Sandbox to workout how Jackson works.
 */
class JacksonTest
{
    static class Event {
        public String name
        public LocalDateTime eventDate
        public Event() {}
        public Event(String name, LocalDateTime date) { this.name=name; eventDate=date; }
    }
    static class Conference {
        public String name
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName="event")
        public List<Event> events
        public Conference() {}
        public Conference(String n, List<Event> e) {name=n;events=e}
    }
    static class Event2 {
        public ZonedDateTime eventDate
    }


    @Test
    public void testSerializingJava8Date() throws IOException {
        LocalDateTime date = LocalDateTime.of(2014, 12, 20, 2, 30)

        ObjectMapper mapper = new XmlMapper()//ObjectMapper()
        JavaTimeModule javaTimeModule = new JavaTimeModule()
        mapper.registerModule(javaTimeModule)

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        String result = mapper.writeValueAsString(date)
        assertThat(result, containsString("2014-12-20T02:30"))

        ZonedDateTime zdate = ZonedDateTime.of(date, ZoneId.of("Europe/Paris"))
        result = mapper.writeValueAsString(zdate)

        Event2 e3 = new Event2(); e3.eventDate = zdate
        result = mapper.writeValueAsString(e3)
        System.out.println("zoneded: "+result)
        assertThat(result, containsString("2014-12-20T02:30"))
        Event2 e4 = mapper.readerFor(Event2.class).readValue(result)
        result = "<Event2><eventDate>2014-12-20T02:30:00</eventDate></Event2>"
        //Event2 e5 = mapper.readerFor(Event2.class).readValue(result)

        Event e = new Event("party", date)
        result = mapper.writeValueAsString(e)
        //System.out.println(result)
        assertThat(result, containsString("2014-12-20T02:30"))
        Event e1 = mapper.readerFor(Event.class).readValue(result)
        assertEquals(date, e1.eventDate)
    }

    @Test
    void testContainerHandling() throws IOException
    {


        ObjectMapper mapper = new XmlMapper()//ObjectMapper()
        mapper.registerModule(new JavaTimeModule())
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

        LocalDateTime date1 = LocalDateTime.of(2014, 12, 20, 2, 30)
        Event e1 = new Event("party", date1)
        LocalDateTime date2 = LocalDateTime.of(2014, 12, 20, 12, 05)
        Event e2 = new Event("key-note", date2)
        Conference c = new Conference("Tech-1", Arrays.asList(e1, e2))
        String result = mapper.writeValueAsString(c)
        System.out.println(result)
        assertThat(result, containsString("2014-12-20T12:05"))
        Conference c1 = mapper.readerFor(Conference.class).readValue(result)
        assertEquals("Tech-1", c1.name)
        assertEquals(2, c1.events.size())


    }

}
