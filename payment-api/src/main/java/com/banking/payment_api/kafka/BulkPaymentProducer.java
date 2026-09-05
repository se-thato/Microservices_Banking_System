package com.banking.payment_api.kafka;

import com.banking.payment_api.dto.BulkPaymentEventDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
// this class publishes events to kafka
public class BulkPaymentProducer {

    private static final Logger logger = LoggerFactory.getLogger(BulkPaymentProducer.class);
    //logger will assist in tracking what get published to Kafka

    private final KafkaTemplate<String, BulkPaymentEventDTO> kafkaTemplate;
    //the KafkaTemplate is the tool that will be used to send messages to Kafka
    //bulkpaymenteventdto if for payment details

    @Value("${kafka.topic.bulk.initiated}")
    private String bulkInitiatedTopic;
    //topic name from app properties, "bulk payment initiated"


    public BulkPaymentProducer(
            KafkaTemplate<String, BulkPaymentEventDTO> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }


    public void publishPaymentEvent(BulkPaymentEventDTO event) {
        //this publishes ONE payment event to kafka, called once per payment in bulk batch

        logger.info("Publishing bulk payment event: batchId={}, ref={}",
                event.getBatchId(), event.getPaymentReference());


        CompletableFuture<SendResult<String, BulkPaymentEventDTO>> future = kafkaTemplate.send(
                bulkInitiatedTopic,
                event.getPaymentReference(),
                event //the actual payment data - JSON format

        );
        //send is async, which returns CompletableFuture, meaning we do not wait for kafka to confirm before continuing


        future.whenComplete((result, exception) -> {
            if (exception != null) {
                //pushing failed
                logger.error("Failed to publish payment event: ref={}, error={}",
                        event.getPaymentReference(),
                        exception.getMessage());

            } else {
                //pushing succeeded
                logger.info("Payment event published: ref={}, offset={}",
                        event.getPaymentReference(),
                        result.getRecordMetadata().partition(), //which partition it went to(0,1 0r 3)
                        result.getRecordMetadata().offset()); //this represent the position in the partition queue
            }
        });
    }
}