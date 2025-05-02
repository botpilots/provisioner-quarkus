package io.hulsbo.util.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import java.util.UUID;

import java.io.IOException;

public class UUIDSerializer extends StdSerializer<UUID> {

	public UUIDSerializer() {
		this(null);
	}

	public UUIDSerializer(Class<UUID> t) {
		super(t);
	}

	@Override
	public void serialize(UUID value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (value != null) {
			gen.writeString(value.toString());
		} else {
			gen.writeNull();
		}
	}
}