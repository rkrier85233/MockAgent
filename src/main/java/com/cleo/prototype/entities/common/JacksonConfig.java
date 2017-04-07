package com.cleo.prototype.entities.common;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.ws.rs.core.Link;

public class JacksonConfig {
    public static class LinkDeserializer extends StdDeserializer<Link> {
        public LinkDeserializer() {
            super(Link.class);
        }

        @Override
        public Link deserialize(JsonParser jp, DeserializationContext ctx) throws IOException {
            ObjectCodec oc = jp.getCodec();
            JsonNode node = oc.readTree(jp);
            final String rel = node.get("rel").asText();
            final String uri = node.get("href").asText();
            Link.Builder builder = Link.fromUri(uri).rel(rel);
            return builder.build();
        }
    }

    public static class LinkSerializer extends StdSerializer<Link> {
        public LinkSerializer() {
            super(Link.class);
        }

        @Override
        public void serialize(Link link, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            jgen.writeStringField("rel", link.getRel());
            jgen.writeStringField("href", link.getUri().toString());
            jgen.writeEndObject();
        }
    }

    public static class DateDeserializer extends JsonDeserializer<Date> {

        @Override
        public Date deserialize(JsonParser jp,
                                DeserializationContext dc) throws IOException, JsonProcessingException {
            ObjectCodec codec = jp.getCodec();
            TextNode node = (TextNode) codec.readTree(jp);
            String dateString = node.textValue();
            Instant instant = Instant.parse(dateString);
            return Date.from(instant);
        }
    }

    public static class DateSerializer extends JsonSerializer<Date> {

        @Override
        public void serialize(Date date, JsonGenerator jg,
                              SerializerProvider sp) throws IOException, JsonProcessingException {
            jg.writeString(DateTimeFormatter.ISO_INSTANT.format(date.toInstant()));
        }
    }
}
