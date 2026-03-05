package ru.alphabank.uvs.validation.persistence;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ValidationEvent(
        Long id,
        UUID requestId,
        OffsetDateTime createdAt,
        OffsetDateTime startAt,
        OffsetDateTime executedAt,
        String modelType,
        String externalId,
        ValidationEventStatus status,
        OffsetDateTime retryAt,
        String data
) {
}
