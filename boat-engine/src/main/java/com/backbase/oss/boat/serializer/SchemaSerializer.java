package com.backbase.oss.boat.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import io.swagger.v3.oas.models.media.Schema;
import java.io.IOException;

public class SchemaSerializer extends JsonSerializer<Schema> {

    @Override
    public void serialize(Schema value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value != null) {
            gen.writeObject(value);
//            gen.writeStartObject();
//            gen.writeObject(value);
            gen.writeEndObject();
        }
    }

}
