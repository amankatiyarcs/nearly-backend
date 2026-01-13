package com.nearly.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response")
public class AuthResponse {
    
    @Schema(description = "Whether the operation was successful")
    private boolean success;
    
    @Schema(description = "Error message if operation failed")
    private String error;
    
    @Schema(description = "JWT access token")
    private String accessToken;
    
    @Schema(description = "JWT refresh token")
    private String refreshToken;
    
    @Schema(description = "Access token type", example = "Bearer")
    private String tokenType;
    
    @Schema(description = "Access token expiration time in seconds")
    private Long expiresIn;
    
    @Schema(description = "User information")
    private UserDto user;
}

