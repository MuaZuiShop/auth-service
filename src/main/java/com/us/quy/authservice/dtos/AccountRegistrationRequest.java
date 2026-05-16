package com.us.quy.authservice.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRegistrationRequest {
    String username;
    String password;
    String email;
}
