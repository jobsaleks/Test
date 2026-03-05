package ru.alphabank.uvs.validation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.alphabank.uvs.validation.api.ValidationRequestGateway;
import ru.alphabank.uvs.validation.dto.ValidationRequestDto;
import ru.alphabank.uvs.validation.persistence.ValidationEventRepository;
import ru.alphabank.uvs.validation.persistence.ValidationEventStatus;

import java.util.UUID;

@Service
public class ValidationService implements ValidationRequestGateway {
    private final ValidationEventRepository repository;
    private final ValidationKafkaProducer producer;
    private final ObjectMapper objectMapper;

    public ValidationService(ValidationEventRepository repository, ValidationKafkaProducer producer, ObjectMapper objectMapper) {
        this.repository = repository;
        this.producer = producer;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public UUID sendValidationRequest(ValidationRequestDto requestDto) {
        UUID requestId = UUID.randomUUID();
        repository.insert(requestId, requestDto.getModelType(), requestDto.getExternalId(), ValidationEventStatus.IN_PROGRESS, null, toJson(requestDto));
        producer.send(requestId, requestDto);
        return requestId;
    }

    String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize object to JSON", ex);
        }
    }
}
