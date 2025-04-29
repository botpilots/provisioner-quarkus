package io.hulsbo.util.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import io.hulsbo.util.model.SafeID;

public class SafeIDModule extends SimpleModule {

	public SafeIDModule() {
		addSerializer(SafeID.class, new SafeIDSerializer());
		addDeserializer(SafeID.class, new SafeIDDeserializer());
	}
}