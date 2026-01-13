package com.nearly.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request payload")
public class LoginRequest {
    
    @NotBlank(message = "Username or email is required")
    @Schema(description = "Username or email address", example = "john_doe")
    private String usernameOrEmail;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "User password", example = "password123")
    private String password;
    
    @Schema(description = "Device information for token tracking")
    private String deviceInfo;
}

