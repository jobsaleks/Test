package ru.alphabank.uvs.validation.kafka;

import org.apache.kafka.clients.producer.ProducerRecord;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ValidationProducerRecordFactory {

    public ProducerRecord<String, byte[]> build(String topic, String replyTopic, UUID requestId, byte[] body) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, requestId.toString(), body);
        record.headers().add("replyTopic", replyTopic.getBytes(StandardCharsets.UTF_8));
        return record;
    }
}
