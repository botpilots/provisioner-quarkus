package io.hulsbo.util.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import java.util.UUID;

public class UUIDModule extends SimpleModule {

	public UUIDModule() {
		addSerializer(UUID.class, new UUIDSerializer());
		addDeserializer(UUID.class, new UUIDDeserializer());
	}
}