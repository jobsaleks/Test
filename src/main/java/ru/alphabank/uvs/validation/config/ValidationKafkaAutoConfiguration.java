package ru.alphabank.uvs.validation.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.ConsumerFactory;
import org.apache.kafka.clients.producer.ProducerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import ru.alphabank.uvs.validation.api.ValidationRsHandler;
import ru.alphabank.uvs.validation.kafka.AvroRecordCodec;
import ru.alphabank.uvs.validation.kafka.ValidationProducerRecordFactory;
import ru.alphabank.uvs.validation.service.ValidationKafkaReplyConsumer;
import ru.alphabank.uvs.validation.service.ValidationRetryScheduler;

@AutoConfiguration
@EnableScheduling
@EnableConfigurationProperties(ValidationKafkaProperties.class)
@ConditionalOnProperty(prefix = "validation.kafka", name = "reply-topic")
@ConditionalOnBean(ValidationRsHandler.class)
public class ValidationKafkaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AvroRecordCodec avroRecordCodec() {
        return new AvroRecordCodec();
    }

    @Bean
    @ConditionalOnMissingBean
    public ValidationProducerRecordFactory validationProducerRecordFactory() {
        return new ValidationProducerRecordFactory();
    }

    @Bean
    public Class<? extends SpecificRecord> validationResponseClass(ValidationKafkaProperties properties) {
        try {
            Class<?> type = Class.forName(properties.getResponseRecordClass());
            if (!SpecificRecord.class.isAssignableFrom(type)) {
                throw new IllegalStateException("validation.kafka.response-record-class must implement SpecificRecord");
            }
            return (Class<? extends SpecificRecord>) type;
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Cannot load response avro class", ex);
        }
    }

    @Bean
    public ValidationKafkaReplyConsumer validationKafkaReplyConsumer(ConsumerFactory<String, byte[]> consumerFactory,
                                                                     ValidationKafkaProperties properties,
                                                                     ru.alphabank.uvs.validation.kafka.ValidationAvroMapper mapper,
                                                                     AvroRecordCodec codec,
                                                                     ru.alphabank.uvs.validation.service.ValidationResponseProcessor processor,
                                                                     Class<? extends SpecificRecord> responseClass) {
        return new ValidationKafkaReplyConsumer(consumerFactory, properties, mapper, codec, processor, responseClass);
    }

    @Bean
    public ValidationRetryScheduler validationRetryScheduler(ru.alphabank.uvs.validation.persistence.ValidationEventRepository repository,
                                                             ru.alphabank.uvs.validation.service.ValidationKafkaProducer producer,
                                                             ValidationKafkaProperties properties,
                                                             ObjectMapper objectMapper) {
        return new ValidationRetryScheduler(repository, producer, properties, objectMapper);
    }
}
