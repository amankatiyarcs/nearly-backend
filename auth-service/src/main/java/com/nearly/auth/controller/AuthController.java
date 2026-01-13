package com.nearly.auth.controller;

import com.nearly.auth.dto.*;
import com.nearly.auth.service.AnonymousSessionService;
import com.nearly.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "JWT Authentication APIs with OTP verification")
public class AuthController {

    private final AuthService authService;
    private final AnonymousSessionService sessionService;

    // ==================== USER AUTHENTICATION ====================

    @Operation(summary = "User Signup", description = "Create a new user account")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Signup successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already exists")
    })
    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        log.info("Signup request for username: {}", request.getUsername());
        AuthResponse response = authService.signup(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "User Login", description = "Authenticate user and get JWT tokens")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login successful",
                    content = @Content(schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest) {
        log.info("Login attempt for: {}", request.getUsernameOrEmail());
        String ipAddress = getClientIp(httpRequest);
        AuthResponse response = authService.login(request, ipAddress);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    @Operation(summary = "Refresh Access Token", description = "Get a new access token using refresh token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token")
    })
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        log.info("Token refresh request");
        AuthResponse response = authService.refreshToken(request.getRefreshToken());
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(401).body(response);
        }
    }

    @Operation(summary = "Logout", description = "Invalidate refresh token")
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(@RequestBody Map<String, String> body) {
        String refreshToken = body.get("refreshToken");
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully"
        ));
    }

    @Operation(summary = "Logout from all devices", description = "Invalidate all refresh tokens for user")
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, Object>> logoutAll(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            authService.validateToken(token).ifPresent(user -> {
                authService.logoutAll(user.getId());
            });
        }
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out from all devices"
        ));
    }

    // ==================== PASSWORD RESET ====================

    @Operation(summary = "Forgot Password", description = "Send OTP to email for password reset")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP sent successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid email format")
    })
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, Object>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Forgot password request for email: {}", request.getEmail());
        Map<String, Object> response = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Verify OTP", description = "Verify OTP code sent to email")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP verification result"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP")
    })
    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        log.info("Verify OTP request for email: {}", request.getEmail());
        Map<String, Object> response = authService.verifyOtp(request.getEmail(), request.getOtp());
        
        if ((Boolean) response.getOrDefault("success", false)) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @Operation(summary = "Reset Password", description = "Reset password with verified OTP")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successful"),
            @ApiResponse(responseCode = "400", description = "Invalid OTP or password")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        log.info("Reset password request for email: {}", request.getEmail());
        Map<String, Object> response = authService.resetPassword(
                request.getEmail(), request.getOtp(), request.getNewPassword());
        
        if ((Boolean) response.getOrDefault("success", false)) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ==================== USERNAME CHECK ====================

    @Operation(summary = "Check Username Availability", description = "Check if username is available and get suggestions")
    @GetMapping("/check-username")
    public ResponseEntity<Map<String, Object>> checkUsername(@RequestParam String username) {
        log.info("Checking username availability: {}", username);
        Map<String, Object> result = authService.checkUsernameAvailability(username);
        return ResponseEntity.ok(result);
    }

    // ==================== TOKEN VALIDATION ====================

    @Operation(summary = "Validate Token", description = "Validate JWT access token")
    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> errorResponse = new java.util.HashMap<>();
            errorResponse.put("valid", false);
            errorResponse.put("error", "Missing or invalid Authorization header");
            return ResponseEntity.status(401).body(errorResponse);
        }

        String token = authHeader.substring(7);
        var userOpt = authService.validateToken(token);
        
        if (userOpt.isPresent()) {
            var user = userOpt.get();
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("valid", true);
            response.put("userId", user.getId());
            response.put("username", user.getUsername());
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new java.util.HashMap<>();
            response.put("valid", false);
            response.put("error", "Invalid or expired token");
            return ResponseEntity.status(401).body(response);
        }
    }

    // ==================== ANONYMOUS SESSION (for Random Chat) ====================

    @Operation(summary = "Create Anonymous Session", description = "Create anonymous session for random chat")
    @PostMapping("/anonymous/session")
    public ResponseEntity<AnonymousSessionResponse> createAnonymousSession(
            @RequestBody AnonymousSessionRequest request) {
        log.info("Creating anonymous session for mode: {}", request.getChatMode());
        AnonymousSessionResponse response = sessionService.createAnonymousSession(request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(429).body(response);
        }
    }

    @Operation(summary = "Validate Anonymous Session", description = "Validate anonymous session ID")
    @GetMapping("/anonymous/session/{sessionId}/validate")
    public ResponseEntity<Map<String, Object>> validateSession(@PathVariable String sessionId) {
        boolean valid = sessionService.validateSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "sessionId", sessionId
        ));
    }

    @Operation(summary = "End Anonymous Session", description = "End and invalidate anonymous session")
    @DeleteMapping("/anonymous/session/{sessionId}")
    public ResponseEntity<Map<String, Object>> endSession(@PathVariable String sessionId) {
        sessionService.invalidateSession(sessionId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Session ended successfully"
        ));
    }

    @Operation(summary = "Extend Anonymous Session", description = "Extend anonymous session duration")
    @PostMapping("/anonymous/session/{sessionId}/extend")
    public ResponseEntity<Map<String, Object>> extendSession(@PathVariable String sessionId) {
        boolean extended = sessionService.extendSession(sessionId);
        if (extended) {
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Session extended"
            ));
        } else {
            return ResponseEntity.status(404).body(Map.of(
                    "success", false,
                    "message", "Session not found or expired"
            ));
        }
    }

    // ==================== HEALTH CHECK ====================

    @Operation(summary = "Health Check", description = "Service health check endpoint")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
