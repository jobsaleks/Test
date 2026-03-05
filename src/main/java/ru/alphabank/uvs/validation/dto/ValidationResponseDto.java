package ru.alphabank.uvs.validation.dto;

import java.util.Map;
import java.util.UUID;

public record ValidationResponseDto(
        UUID requestId,
        ValidationResponseStatus status,
        String message,
        Map<String, Object> payload
) {
}
