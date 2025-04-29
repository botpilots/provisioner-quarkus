package io.hulsbo.util.jackson;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CustomJacksonProvider implements ContextResolver<ObjectMapper> {

	private final ObjectMapper objectMapper;

	public CustomJacksonProvider() {
		objectMapper = new ObjectMapper();
		objectMapper.registerModule(new SafeIDModule());
		objectMapper.registerModule(new JavaTimeModule());
		objectMapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
		objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}

	@Override
	public ObjectMapper getContext(Class<?> type) {
		return objectMapper;
	}
}