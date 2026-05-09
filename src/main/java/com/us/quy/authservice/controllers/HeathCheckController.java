
package com.us.quy.authservice.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health-check")
@Tag(name = "Health Check", description = "API dùng để kiểm tra trạng thái hoạt động của hệ thống")
public class HeathCheckController {

    @GetMapping
    @Operation(
        summary = "Kiểm tra trạng thái (Health check)",
        description = "Trả về thông điệp 'Hello World!, I'm AUTH SERVICE' "
                + "để xác nhận rằng service đang hoạt động bình thường."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Service hoạt động bình thường",
            content = @Content(mediaType = "text/plain")
    )
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("Hello World!, I'm AUTH SERVICE");
    }
}