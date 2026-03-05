# validation-kafka-starter

Spring Boot starter (Java 17, Spring Boot 3.5.7) для отправки validation-запросов в Kafka,
приема ответов, обработки commit/rollback, сохранения событий в PostgreSQL через `JdbcTemplate`
и retry-механизма без `@KafkaListener`.

## Основные возможности

- Публичные DTO библиотеки (`ValidationRequestDto`, `ValidationResponseDto`) — Avro-классы наружу не отдаются.
- API `ValidationRequestGateway#sendValidationRequest`:
    - генерирует `requestId`;
    - пишет событие в `validation_events` (`JSONB data`, `IN_PROGRESS`);
    - отправляет запрос в Kafka topic (`validation.kafka.request-topic`) c header `replyTopic`.
- Ручной Kafka-consumer без `@KafkaListener`, с concurrency.
- `ValidationRsHandler` получает `ValidationResponseWrapper` и может вызвать `commit()` / `rollback()`.
- Если handler не вызвал решение — по умолчанию `commit()`.
- Retry scheduler на `select ... for update skip locked` для записей `WAITING` и `retry_at <= now()`.
- Liquibase changelog создания таблицы и индексов (`request_id`, `status`, `retry_at`).

## Подключение

Стартер активируется только если:

1. задан `validation.kafka.reply-topic`;
2. в контексте есть бин `ValidationRsHandler`;
3. в контексте есть бин `ValidationAvroMapper`.

## Конфигурация

```yaml
validation:
  kafka:
    request-topic: validation.requests
    reply-topic: validation.replies
    consumer-group: validation-starter
    consumer-concurrency: 2
    retry-batch-size: 100
    retry-delay: 5m
    scheduler-delay-ms: 10000
    consumer-poll-timeout: 1s
    response-record-class: ru.alphabank.uvs.api.ValidationRs
```

## Контракты клиента

```java
public interface ValidationRsHandler {
    void handle(ValidationResponseWrapper response);
}
```

```java
public interface ValidationAvroMapper {
    SpecificRecord toRequestRecord(ValidationRequestDto requestDto);
    ValidationResponseDto fromResponseRecord(SpecificRecord specificRecord);
}
```

`ValidationResponseWrapper` предоставляет:

- `response()` — DTO ответа;
- `commit()` — зафиксировать `SUCCESS`;
- `rollback()` — `CANCELLED` + новая запись `WAITING`.

## Liquibase

Подключайте changelog:

```yaml
spring:
  liquibase:
    change-log: classpath:/db/changelog/db.changelog-master.yaml
```
