package com.us.quy.authservice.controllers;

import com.us.quy.authservice.dtos.AccountRegistrationRequest;
import com.us.quy.authservice.services.AccountService;
import com.us.quy.authservice.utils.Message;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/register")
    ResponseEntity register(@RequestBody AccountRegistrationRequest request) {
        accountService.register(request);
        return ResponseEntity.ok(Message.REGISTER_SUCCESSFULLY);
    }
}
