package ru.alphabank.uvs.validation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.scheduling.annotation.Scheduled;
import ru.alphabank.uvs.validation.config.ValidationKafkaProperties;
import ru.alphabank.uvs.validation.dto.ValidationRequestDto;
import ru.alphabank.uvs.validation.persistence.ValidationEvent;
import ru.alphabank.uvs.validation.persistence.ValidationEventRepository;

import java.util.List;
import java.util.Map;

public class ValidationRetryScheduler {
    private final ValidationEventRepository repository;
    private final ValidationKafkaProducer producer;
    private final ValidationKafkaProperties properties;
    private final ObjectMapper objectMapper;

    public ValidationRetryScheduler(ValidationEventRepository repository,
                                    ValidationKafkaProducer producer,
                                    ValidationKafkaProperties properties,
                                    ObjectMapper objectMapper) {
        this.repository = repository;
        this.producer = producer;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${validation.kafka.scheduler-delay-ms:10000}")
    public void retryWaiting() {
        List<ValidationEvent> events = repository.lockReadyForRetry(properties.getRetryBatchSize());
        for (ValidationEvent event : events) {
            producer.send(event.requestId(), fromJson(event.data()));
        }
    }

    @SuppressWarnings("unchecked")
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
            throw new IllegalStateException("Cannot parse retry payload", ex);
        }
    }
}
