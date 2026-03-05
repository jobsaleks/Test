package ru.alphabank.uvs.validation.service;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerFactory;
import org.apache.kafka.common.KafkaException;
import org.apache.avro.specific.SpecificRecord;
import org.springframework.stereotype.Component;
import ru.alphabank.uvs.validation.config.ValidationKafkaProperties;
import ru.alphabank.uvs.validation.dto.ValidationRequestDto;
import ru.alphabank.uvs.validation.kafka.AvroRecordCodec;
import ru.alphabank.uvs.validation.kafka.ValidationAvroMapper;
import ru.alphabank.uvs.validation.kafka.ValidationProducerRecordFactory;

import java.util.UUID;

@Component
public class ValidationKafkaProducer {
    private final ProducerFactory<String, byte[]> producerFactory;
    private final ValidationKafkaProperties properties;
    private final ValidationAvroMapper avroMapper;
    private final ValidationProducerRecordFactory recordFactory;
    private final AvroRecordCodec avroRecordCodec;

    public ValidationKafkaProducer(ProducerFactory<String, byte[]> producerFactory,
                                   ValidationKafkaProperties properties,
                                   ValidationAvroMapper avroMapper,
                                   ValidationProducerRecordFactory recordFactory,
                                   AvroRecordCodec avroRecordCodec) {
        this.producerFactory = producerFactory;
        this.properties = properties;
        this.avroMapper = avroMapper;
        this.recordFactory = recordFactory;
        this.avroRecordCodec = avroRecordCodec;
    }

    public void send(UUID requestId, ValidationRequestDto requestDto) {
        SpecificRecord requestRecord = avroMapper.toRequestRecord(requestDto);
        byte[] payload = avroRecordCodec.serialize(requestRecord);

        try (Producer<String, byte[]> producer = producerFactory.createProducer()) {
            producer.send(recordFactory.build(properties.getRequestTopic(), properties.getReplyTopic(), requestId, payload));
            producer.flush();
        } catch (Exception ex) {
            throw new KafkaException("Cannot send validation request", ex);
        }
    }
}
