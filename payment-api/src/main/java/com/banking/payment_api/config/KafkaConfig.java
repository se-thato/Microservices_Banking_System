package com.banking.payment_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topic.bulk.initiated}")
    private String bulkInitiatedTopic;

    @Value("${kafka.topic.payment.completed}")
    private String paymentCompleteTopic;

    @Value("${kafka.topic.payment.failed}")
    private String paymentFailedTopic;


    @Bean
    public NewTopic bulkInitiatedTopic() {
        return TopicBuilder
                .name(bulkInitiatedTopic) //topic name from properties
                .partitions(3)
                //so partions is important for handling large salary runs, e.g 300 payments splits accross 3 partitions
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentCompletedTopic() {
        return TopicBuilder
                .name(paymentCompleteTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }


    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder
                .name(paymentFailedTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}