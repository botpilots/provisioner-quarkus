package io.hulsbo.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.hulsbo.util.model.SafeID;

import java.io.IOException;

public class SafeIDDeserializer extends StdDeserializer<SafeID> {

	public SafeIDDeserializer() {
		this(null);
	}

	public SafeIDDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public SafeID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		String id = p.getValueAsString();
		if (id == null || id.isEmpty()) {
			return null;
		}
		return SafeID.fromString(id);
	}
}