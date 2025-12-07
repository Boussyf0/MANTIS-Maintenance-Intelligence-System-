package com.mantis.preprocessing.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${app.topic.output}")
    private String outputTopic;

    @Bean
    public NewTopic preprocessedDataTopic() {
        return TopicBuilder.name(outputTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
