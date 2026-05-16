package com.us.quy.authservice.kafka.providers;

import com.us.quy.authservice.entities.AccountEntity;
import com.us.quy.authservice.kafka.EventType;
import com.us.quy.authservice.kafka.RegisteredEventMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AccountPublisherImpl implements AccountPublisher {
    private final KafkaTemplate<String, RegisteredEventMessage> kafkaTemplate;

    @Override
    public void registerEvent(String topic, AccountEntity account) {
        RegisteredEventMessage message = RegisteredEventMessage.builder()
            .eventType(EventType.ACCOUNT_REEGISTERED)
            .data(RegisteredEventMessage.Account.builder()
                .id(account.getId())
                .email(account.getEmail())
                .username(account.getUsername())
                .status(account.getStatus())
                .createdAt(account.getCreatedAt())
                .build())
            .additionalData(null)
            .build();

        kafkaTemplate.send(topic, message);
    }
}
