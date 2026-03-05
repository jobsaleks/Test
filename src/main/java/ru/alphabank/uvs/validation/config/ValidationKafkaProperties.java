package ru.alphabank.uvs.validation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "validation.kafka")
public class ValidationKafkaProperties {
    private String requestTopic = "validation.requests";
    private String replyTopic;
    private String consumerGroup = "validation-starter";
    private int consumerConcurrency = 2;
    private int retryBatchSize = 100;
    private Duration retryDelay = Duration.ofMinutes(5);
    private long schedulerDelayMs = 10_000L;
    private Duration consumerPollTimeout = Duration.ofSeconds(1);
    private String responseRecordClass;

    public String getRequestTopic() { return requestTopic; }
    public void setRequestTopic(String requestTopic) { this.requestTopic = requestTopic; }
    public String getReplyTopic() { return replyTopic; }
    public void setReplyTopic(String replyTopic) { this.replyTopic = replyTopic; }
    public String getConsumerGroup() { return consumerGroup; }
    public void setConsumerGroup(String consumerGroup) { this.consumerGroup = consumerGroup; }
    public int getConsumerConcurrency() { return consumerConcurrency; }
    public void setConsumerConcurrency(int consumerConcurrency) { this.consumerConcurrency = consumerConcurrency; }
    public int getRetryBatchSize() { return retryBatchSize; }
    public void setRetryBatchSize(int retryBatchSize) { this.retryBatchSize = retryBatchSize; }
    public Duration getRetryDelay() { return retryDelay; }
    public void setRetryDelay(Duration retryDelay) { this.retryDelay = retryDelay; }
    public long getSchedulerDelayMs() { return schedulerDelayMs; }
    public void setSchedulerDelayMs(long schedulerDelayMs) { this.schedulerDelayMs = schedulerDelayMs; }
    public Duration getConsumerPollTimeout() { return consumerPollTimeout; }
    public void setConsumerPollTimeout(Duration consumerPollTimeout) { this.consumerPollTimeout = consumerPollTimeout; }
    public String getResponseRecordClass() { return responseRecordClass; }
    public void setResponseRecordClass(String responseRecordClass) { this.responseRecordClass = responseRecordClass; }
}
