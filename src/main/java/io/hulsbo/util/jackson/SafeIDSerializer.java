package io.hulsbo.util.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import io.hulsbo.util.model.SafeID;

import java.io.IOException;

public class SafeIDSerializer extends StdSerializer<SafeID> {

	public SafeIDSerializer() {
		this(null);
	}

	public SafeIDSerializer(Class<SafeID> t) {
		super(t);
	}

	@Override
	public void serialize(SafeID value, JsonGenerator gen, SerializerProvider provider) throws IOException {
		if (value != null) {
			gen.writeString(value.toString());
		} else {
			gen.writeNull();
		}
	}
}