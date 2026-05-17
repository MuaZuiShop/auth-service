package com.us.quy.authservice.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Username không được để trống!")
    String username;

    @NotBlank(message = "Mật khẩu không được để trống!")
    String password;

}
