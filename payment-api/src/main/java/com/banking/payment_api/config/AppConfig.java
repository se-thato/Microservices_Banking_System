package com.banking.payment_api.config;
// ☝️ make sure this matches YOUR actual package name

import com.banking.payment_api.dto.BulkPaymentEventDTO;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class AppConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    // reads localhost:9092 from application.properties

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    // reads payment-api-group from application.properties


    //Rest template
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
        //this is used for HTTP calls to Banking API and Customer API
    }


    //Kafka Producer Beans

    @Bean
    public ProducerFactory<String, BulkPaymentEventDTO> producerFactory() {
        // tells Kafka HOW to connect and HOW to serialize messages

        Map<String, Object> config = new HashMap<>();

        config.put(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );
        // where Kafka is running: localhost:9092

        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class
        );
        // payment reference key → serialize as String

        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                JsonSerializer.class
        );
        // BulkPaymentEventDTO value → serialize as JSON

        config.put(
                JsonSerializer.ADD_TYPE_INFO_HEADERS,
                false
        );
        // don't add Java class headers to messages
        // keeps messages clean

        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, BulkPaymentEventDTO> kafkaTemplate() {
        // This is the bean BulkPaymentProducer needs
        // then will inject this automatically by spring

        return new KafkaTemplate<>(producerFactory());
    }


    //Kafka Consumer Beans

    @Bean
    public ConsumerFactory<String, BulkPaymentEventDTO> consumerFactory() {
        //this tells Kafka HOW to read and deserialize messages

        Map<String, Object> config = new HashMap<>();

        config.put(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
                bootstrapServers
        );

        config.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                groupId
        );
        // payment-api-group
        // Kafka remembers which messages this group processed

        config.put(
                ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                "earliest"
        );
        // if no previous position found then start from beginning
        // ensures no messages missed on restart

        config.put(
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                StringDeserializer.class
        );

        config.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                JsonDeserializer.class
        );

        config.put(
                JsonDeserializer.TRUSTED_PACKAGES,
                "*"
        );
        // trust all packages when converting JSON back to object

        config.put(
                JsonDeserializer.VALUE_DEFAULT_TYPE,
                BulkPaymentEventDTO.class.getName()
        );
        // tell deserializer: JSON → BulkPaymentEventDTO

        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, BulkPaymentEventDTO>
    kafkaListenerContainerFactory() {
        // factory that powers @KafkaListener annotation
        // Spring uses this to create listener containers

        ConcurrentKafkaListenerContainerFactory<String, BulkPaymentEventDTO>
                factory = new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory());

        return factory;
    }
}