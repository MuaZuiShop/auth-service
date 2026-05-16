package com.us.quy.authservice.dtos;

import com.us.quy.authservice.enums.ERole;
import com.us.quy.authservice.enums.EStatus;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Account {
    private UUID id;
    private String username;
    private String password;
    private String email;

    private List<ERole> roles = new ArrayList<>();
    private EStatus status;
    private ZonedDateTime emailVerifiedAt;
    private ZonedDateTime profileUpdatedAt;
}
