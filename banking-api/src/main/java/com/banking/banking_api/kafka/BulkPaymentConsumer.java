package com.banking.banking_api.kafka;

import com.banking.banking_api.dto.DebitCreditRequestDTO;
import com.banking.banking_api.dto.BulkPaymentEventDTO;
import com.banking.banking_api.exception.BusinessException;
import com.banking.banking_api.service.BankingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;


// BulkPaymentConsumer will listen to Kafka topic "payment.bulk.initiated"
// Processes each payment event one by one
// This is like a bank teller who processes each salary payment
// Runs in the BACKGROUND


@Component
public class BulkPaymentConsumer {

    private static final Logger logger =
            LoggerFactory.getLogger(BulkPaymentConsumer.class);

    private final BankingService bankingService;

    public BulkPaymentConsumer(BankingService bankingService) {
        this.bankingService = bankingService;
    }


    @KafkaListener(
            topics = "${kafka.topic.bulk.initiated}",
            // this listens to "payment.bulk.initiated" topic
            groupId = "${spring.kafka.consumer.group-id}",

            containerFactory = "kafkaListenerContainerFactory"
    )
    public void processPaymentEvent(
            @Payload BulkPaymentEventDTO event) {
        // @Payload is the actual payment data from Kafka
        // called automatically whenever a new message arrives
        // Spring Kafka handles threading and polling

        logger.info(
                "Received bulk payment event: batchId={}, ref={}, amount={}",
                event.getBatchId(),
                event.getPaymentReference(),
                event.getAmount()
        );

        try {
            // first we'll Look up receiver account
            var receiverAccount = bankingService
                    .getAccountByAccountNumber(event.getToAccountNumber());
            // find receiver's account details

            Long receiverAccountId =
                    receiverAccount.getId();
            //get receiver's account ID for debit/credit

            //Debit sender
            DebitCreditRequestDTO debitRequest =
                    DebitCreditRequestDTO.builder()
                            .accountId(event.getFromAccountId())
                            .amount(event.getAmount())
                            .description("Bulk payment to " +
                                    event.getToAccountNumber() +
                                    " - " + event.getDescription())
                            .transactionReference(
                                    event.getPaymentReference() + "-D"
                            )
                            //the "-D" suffix is for debit transaction
                            .build();

            bankingService.debitAccount(debitRequest);

            //Credit receiver
            DebitCreditRequestDTO creditRequest =
                    DebitCreditRequestDTO.builder()
                            .accountId(receiverAccountId)
                            .amount(event.getAmount())
                            .description("Bulk payment from " +
                                    event.getFromAccountNumber() +
                                    " - " + event.getDescription())
                            .transactionReference(
                                    event.getPaymentReference() + "-C"
                            )
                            //"-C" suffix is for credit transaction
                            .build();

            bankingService.creditAccount(creditRequest);

            logger.info(
                    "Bulk payment processed successfully: ref={}",
                    event.getPaymentReference()
            );

        } catch (Exception e) {
            logger.error(
                    "Failed to process bulk payment: ref={}, error={}",
                    event.getPaymentReference(),
                    e.getMessage()
            );
            // log the failure
            // failed payment stays as PENDING in database
            // admin can investigate and retry
        }
    }
}