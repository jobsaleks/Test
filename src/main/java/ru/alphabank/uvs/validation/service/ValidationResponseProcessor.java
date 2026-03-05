package ru.alphabank.uvs.validation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.alphabank.uvs.validation.api.ValidationResponseWrapper;
import ru.alphabank.uvs.validation.api.ValidationRsHandler;
import ru.alphabank.uvs.validation.config.ValidationKafkaProperties;
import ru.alphabank.uvs.validation.dto.ValidationRequestDto;
import ru.alphabank.uvs.validation.dto.ValidationResponseDto;
import ru.alphabank.uvs.validation.dto.ValidationResponseStatus;
import ru.alphabank.uvs.validation.persistence.ValidationEvent;
import ru.alphabank.uvs.validation.persistence.ValidationEventRepository;
import ru.alphabank.uvs.validation.persistence.ValidationEventStatus;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Component
public class ValidationResponseProcessor {
    private final ValidationRsHandler handler;
    private final ValidationEventRepository repository;
    private final ValidationKafkaProperties properties;
    private final ObjectMapper objectMapper;

    public ValidationResponseProcessor(ValidationRsHandler handler,
                                       ValidationEventRepository repository,
                                       ValidationKafkaProperties properties,
                                       ObjectMapper objectMapper) {
        this.handler = handler;
        this.repository = repository;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void process(ValidationResponseDto response) {
        UUID requestId = response.requestId();
        ValidationEvent current = repository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalStateException("Validation event not found: " + requestId));

        if (response.status() == ValidationResponseStatus.SUCCESS) {
            handleSuccess(response, current);
            return;
        }
        if (response.status() == ValidationResponseStatus.REJECTED) {
            performRollback(current);
            return;
        }
        repository.updateStatus(requestId, ValidationEventStatus.ERROR);
        createRetry(current, OffsetDateTime.now());
    }

    private void handleSuccess(ValidationResponseDto response, ValidationEvent event) {
        ValidationResponseWrapper wrapper = new ValidationResponseWrapper(response);
        handler.handle(wrapper);
        if (wrapper.decisionOrDefaultCommit() == ValidationResponseWrapper.Decision.ROLLBACK) {
            performRollback(event);
        } else {
            repository.updateStatus(event.requestId(), ValidationEventStatus.SUCCESS);
        }
    }

    private void performRollback(ValidationEvent event) {
        repository.updateStatus(event.requestId(), ValidationEventStatus.CANCELLED);
        createRetry(event, OffsetDateTime.now().plus(properties.getRetryDelay()));
    }

    private void createRetry(ValidationEvent event, OffsetDateTime retryAt) {
        ValidationRequestDto dto = fromJson(event.data());
        UUID newRequestId = UUID.randomUUID();
        repository.insert(newRequestId, event.modelType(), event.externalId(), ValidationEventStatus.WAITING, retryAt, toJson(dto));
    }

    private ValidationRequestDto fromJson(String json) {
        try {
            Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {
            });
            return ValidationRequestDto.builder()
                    .modelType((String) map.get("modelType"))
                    .externalId((String) map.get("externalId"))
                    .payload((Map<String, Object>) map.getOrDefault("payload", Map.of()))
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot parse request JSON", ex);
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot serialize request JSON", ex);
        }
    }
}
