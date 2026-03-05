package ru.alphabank.uvs.validation.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class ValidationRequestDto {
    private final String modelType;
    private final String externalId;
    private final Map<String, Object> payload;

    private ValidationRequestDto(Builder builder) {
        this.modelType = builder.modelType;
        this.externalId = builder.externalId;
        this.payload = Collections.unmodifiableMap(new HashMap<>(builder.payload));
    }

    public String getModelType() {
        return modelType;
    }

    public String getExternalId() {
        return externalId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String modelType;
        private String externalId;
        private final Map<String, Object> payload = new HashMap<>();

        public Builder modelType(String modelType) {
            this.modelType = modelType;
            return this;
        }

        public Builder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public Builder putPayload(String key, Object value) {
            this.payload.put(key, value);
            return this;
        }

        public Builder payload(Map<String, Object> payload) {
            this.payload.clear();
            this.payload.putAll(payload);
            return this;
        }

        public ValidationRequestDto build() {
            return new ValidationRequestDto(this);
        }
    }
}
