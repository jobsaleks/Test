package ru.alphabank.uvs.validation.service;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerFactory;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.context.SmartLifecycle;
import ru.alphabank.uvs.validation.config.ValidationKafkaProperties;
import ru.alphabank.uvs.validation.dto.ValidationResponseDto;
import ru.alphabank.uvs.validation.kafka.AvroRecordCodec;
import ru.alphabank.uvs.validation.kafka.ValidationAvroMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ValidationKafkaReplyConsumer implements SmartLifecycle {
    private final ConsumerFactory<String, byte[]> consumerFactory;
    private final ValidationKafkaProperties properties;
    private final ValidationAvroMapper mapper;
    private final AvroRecordCodec codec;
    private final ValidationResponseProcessor processor;
    private final Class<? extends SpecificRecord> responseClass;

    private volatile boolean running;
    private ExecutorService executor;

    public ValidationKafkaReplyConsumer(ConsumerFactory<String, byte[]> consumerFactory,
                                        ValidationKafkaProperties properties,
                                        ValidationAvroMapper mapper,
                                        AvroRecordCodec codec,
                                        ValidationResponseProcessor processor,
                                        Class<? extends SpecificRecord> responseClass) {
        this.consumerFactory = consumerFactory;
        this.properties = properties;
        this.mapper = mapper;
        this.codec = codec;
        this.processor = processor;
        this.responseClass = responseClass;
    }

    @Override
    public void start() {
        if (running) {
            return;
        }
        running = true;
        executor = Executors.newFixedThreadPool(properties.getConsumerConcurrency());
        for (int i = 0; i < properties.getConsumerConcurrency(); i++) {
            executor.submit(this::runConsumer);
        }
    }

    private void runConsumer() {
        try (Consumer<String, byte[]> consumer = consumerFactory.createConsumer(properties.getConsumerGroup(), null)) {
            consumer.subscribe(List.of(properties.getReplyTopic()));
            while (running) {
                ConsumerRecords<String, byte[]> records = consumer.poll(Duration.ofMillis(properties.getConsumerPollTimeout().toMillis()));
                for (ConsumerRecord<String, byte[]> record : records) {
                    SpecificRecord specificRecord = decode(record.value());
                    ValidationResponseDto response = mapper.fromResponseRecord(specificRecord);
                    processor.process(response);
                }
                consumer.commitSync();
            }
        }
    }

    private SpecificRecord decode(byte[] value) {
        try {
            Schema schema = (Schema) responseClass.getMethod("getClassSchema").invoke(null);
            return codec.deserialize(value, schema, responseClass);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot decode avro response", ex);
        }
    }

    @Override
    public void stop() {
        running = false;
        if (executor != null) {
            executor.shutdown();
            try {
                executor.awaitTermination(10, TimeUnit.SECONDS);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }
}
