package com.us.quy.authservice.kafka.providers;

import com.us.quy.authservice.entities.AccountEntity;
import org.springframework.stereotype.Component;

@Component
public interface AccountPublisher {
    void registerEvent(String topic, AccountEntity account);
}
