package com.us.quy.authservice.kafka;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.us.quy.authservice.enums.EStatus;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RegisteredEventMessage {
    EventType eventType;
    Account data;
    Map<String, String> additionalData;

    @Getter
    @Setter
    @Builder
    public static class Account {
        private UUID id;
        private String username;
        private String email;
        private EStatus status;

        @JsonFormat(
            shape = JsonFormat.Shape.STRING,
            pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX",
            timezone = "Asia/Ho_Chi_Minh"
        )
        private ZonedDateTime createdAt;
    }
}
