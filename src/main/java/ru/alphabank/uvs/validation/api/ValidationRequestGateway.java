package ru.alphabank.uvs.validation.api;

import ru.alphabank.uvs.validation.dto.ValidationRequestDto;

import java.util.UUID;

public interface ValidationRequestGateway {
    UUID sendValidationRequest(ValidationRequestDto requestDto);
}
