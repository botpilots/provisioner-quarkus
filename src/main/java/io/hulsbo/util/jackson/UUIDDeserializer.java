package io.hulsbo.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import java.util.UUID;

import java.io.IOException;

public class UUIDDeserializer extends StdDeserializer<UUID> {

	public UUIDDeserializer() {
		this(null);
	}

	public UUIDDeserializer(Class<?> vc) {
		super(vc);
	}

	@Override
	public UUID deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
		String id = p.getValueAsString();
		if (id == null || id.isEmpty()) {
			return null;
		}
		return UUID.fromString(id);
	}
}