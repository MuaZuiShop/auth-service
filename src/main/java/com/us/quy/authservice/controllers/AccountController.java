package com.us.quy.authservice.controllers;

import com.quy.common.core.exception.ApiResponse;
import com.us.quy.authservice.dtos.AccountRegistrationRequest;
import com.us.quy.authservice.dtos.LoginRequest;
import com.us.quy.authservice.dtos.LoginResponse;
import com.us.quy.authservice.dtos.RefreshTokenRequest;
import com.us.quy.authservice.services.AccountService;
import com.us.quy.authservice.utils.Message;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
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

    @Operation(summary = "Đăng ký tài khoản mới",
        description = "Tạo một tài khoản người dùng mới. Bắn event Kafka sau khi tạo thành công.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Đăng ký thành công",
            content = @Content(schema = @Schema(implementation = String.class, example = "REGISTER_SUCCESSFULLY"))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Dữ liệu đầu vào không hợp lệ", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
            description = "Lỗi trùng lặp: Username hoặc Email đã tồn tại (AUTH_USERNAME_EXISTED / AUTH_EMAIL_EXISTED)",
            content = @Content)
    })
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody AccountRegistrationRequest request) {
        accountService.register(request);
        return ResponseEntity.ok(Message.REGISTER_SUCCESSFULLY);
    }

    @Operation(summary = "Đăng nhập",
        description = "Xác thực bằng Username/Email và Mật khẩu.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Đăng nhập thành công, trả về Access Token và Refresh Token",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Lỗi validation payload", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Sai mật khẩu (AUTH_PASSWORD_WRONG)", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
            description = "Tài khoản chưa kích hoạt hoặc bị khoá (AUTH_ACCOUNT_DISABLED / AUTH_ACCOUNT_LOCKED)",
            content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Không tìm thấy tài khoản hoặc tài khoản đã bị xoá (CUSTOMER_NOT_FOUND / NOT_FOUND)",
            content = @Content)
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = accountService.login(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Làm mới Access Token",
        description = "Cấp mới Access Token và Refresh Token dựa trên Refresh Token cũ.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Làm mới thành công, trả về cặp Token mới",
            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Payload gửi lên không hợp lệ", content = @Content),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
            description = "Token không hợp lệ, hết hạn, hoặc bị đăng xuất từ thiết bị khác "
                + "(AUTH_REFRESH_TOKEN_INVALID / AUTH_REFRESH_TOKEN_EXPIRED / AUTH_LOGIN_FAILED)",
            content = @Content)
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        LoginResponse tokenResponse = accountService.refreshToken(request);
        return ResponseEntity.ok(tokenResponse);
    }

    @Operation(summary = "Đăng xuất",
        description = "Xóa bỏ Refresh Token hiện tại trong Redis để vô hiệu hóa phiên đăng nhập.")
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200",
            description = "Đăng xuất thành công",
            content = @Content(schema = @Schema(implementation = ApiResponse.class))),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
            description = "Payload không hợp lệ",
            content = @Content)
    })
    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody RefreshTokenRequest request) {
        accountService.logout(request);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
