package ru.alphabank.uvs.validation.persistence;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class ValidationEventRepository {
    private final JdbcTemplate jdbcTemplate;

    public ValidationEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void insert(UUID requestId, String modelType, String externalId, ValidationEventStatus status, OffsetDateTime retryAt, String jsonData) {
        jdbcTemplate.update("""
                insert into validation_events(request_id, created_at, start_at, model_type, external_id, status, retry_at, data)
                values (?, now(), now(), ?, ?, ?, ?, cast(? as jsonb))
                """, requestId, modelType, externalId, status.name(), retryAt, jsonData);
    }

    public Optional<ValidationEvent> findByRequestId(UUID requestId) {
        List<ValidationEvent> list = jdbcTemplate.query("""
                select id, request_id, created_at, start_at, executed_at, model_type, external_id, status, retry_at, data::text as data
                from validation_events
                where request_id = ?
                order by id desc
                limit 1
                """, this::mapRow, requestId);
        return list.stream().findFirst();
    }

    public void updateStatus(UUID requestId, ValidationEventStatus status) {
        jdbcTemplate.update("update validation_events set status = ?, executed_at = now() where request_id = ?", status.name(), requestId);
    }

    @Transactional
    public List<ValidationEvent> lockReadyForRetry(int batchSize) {
        List<ValidationEvent> events = jdbcTemplate.query("""
                select id, request_id, created_at, start_at, executed_at, model_type, external_id, status, retry_at, data::text as data
                from validation_events
                where status = 'WAITING' and retry_at <= now()
                order by retry_at
                for update skip locked
                limit ?
                """, this::mapRow, batchSize);

        for (ValidationEvent event : events) {
            jdbcTemplate.update("update validation_events set status = 'IN_PROGRESS', start_at = now() where id = ?", event.id());
        }
        return events;
    }

    private ValidationEvent mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new ValidationEvent(
                rs.getLong("id"),
                rs.getObject("request_id", UUID.class),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("start_at", OffsetDateTime.class),
                rs.getObject("executed_at", OffsetDateTime.class),
                rs.getString("model_type"),
                rs.getString("external_id"),
                ValidationEventStatus.valueOf(rs.getString("status")),
                rs.getObject("retry_at", OffsetDateTime.class),
                rs.getString("data")
        );
    }
}
