package com.nearly.auth.service;

import com.nearly.auth.dto.*;
import com.nearly.auth.model.OtpToken;
import com.nearly.auth.model.RefreshToken;
import com.nearly.auth.model.User;
import com.nearly.auth.repository.RefreshTokenRepository;
import com.nearly.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse signup(SignupRequest request) {
        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return AuthResponse.builder()
                    .success(false)
                    .error("Email already exists")
                    .build();
        }

        // Check if username exists
        if (userRepository.existsByUsername(request.getUsername())) {
            return AuthResponse.builder()
                    .success(false)
                    .error("Username already exists")
                    .build();
        }

        // Create user
        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .bio(request.getBio())
                .location(request.getLocation())
                .avatarUrl(request.getAvatarUrl() != null ? request.getAvatarUrl() : 
                        "https://api.dicebear.com/7.x/avataaars/svg?seed=" + request.getUsername())
                .interests(request.getInterests())
                .followersCount(0)
                .followingCount(0)
                .postsCount(0)
                .isPrivate(false)
                .isVerified(false)
                .showActivityStatus(true)
                .allowStorySharing(true)
                .messagePrivacy("everyone")
                .isActive(true)
                .emailVerified(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created: {}", savedUser.getUsername());

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(savedUser.getId(), savedUser.getUsername(), savedUser.getEmail());
        String refreshToken = jwtService.generateRefreshToken(savedUser.getId());

        // Save refresh token
        saveRefreshToken(savedUser.getId(), refreshToken, null, null);

        // Send welcome email
        emailService.sendWelcomeEmail(savedUser.getEmail(), savedUser.getName());

        return AuthResponse.builder()
                .success(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(toUserDto(savedUser))
                .build();
    }

    public AuthResponse login(LoginRequest request, String ipAddress) {
        // Find user by username or email
        Optional<User> userOpt = userRepository.findByUsernameOrEmail(
                request.getUsernameOrEmail(), request.getUsernameOrEmail());

        if (userOpt.isEmpty()) {
            log.warn("Login failed: user not found for {}", request.getUsernameOrEmail());
            return AuthResponse.builder()
                    .success(false)
                    .error("Invalid username or password")
                    .build();
        }

        User user = userOpt.get();

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: invalid password for {}", request.getUsernameOrEmail());
            return AuthResponse.builder()
                    .success(false)
                    .error("Invalid username or password")
                    .build();
        }

        // Check if user is active
        if (!user.isActive()) {
            return AuthResponse.builder()
                    .success(false)
                    .error("Account is deactivated. Please contact support.")
                    .build();
        }

        // Update last active
        user.setLastActiveAt(Instant.now());
        userRepository.save(user);

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId());

        // Save refresh token
        saveRefreshToken(user.getId(), refreshToken, request.getDeviceInfo(), ipAddress);

        log.info("Login successful for user: {}", user.getUsername());

        return AuthResponse.builder()
                .success(true)
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(toUserDto(user))
                .build();
    }

    public AuthResponse refreshToken(String refreshTokenStr) {
        // Validate refresh token
        if (!jwtService.isTokenValid(refreshTokenStr)) {
            return AuthResponse.builder()
                    .success(false)
                    .error("Invalid or expired refresh token")
                    .build();
        }

        // Check token type
        String tokenType = jwtService.extractTokenType(refreshTokenStr);
        if (!"refresh".equals(tokenType)) {
            return AuthResponse.builder()
                    .success(false)
                    .error("Invalid token type")
                    .build();
        }

        // Find stored refresh token
        Optional<RefreshToken> storedTokenOpt = refreshTokenRepository.findByToken(refreshTokenStr);
        if (storedTokenOpt.isEmpty()) {
            return AuthResponse.builder()
                    .success(false)
                    .error("Refresh token not found")
                    .build();
        }

        RefreshToken storedToken = storedTokenOpt.get();
        String userId = storedToken.getUserId();

        // Find user
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return AuthResponse.builder()
                    .success(false)
                    .error("User not found")
                    .build();
        }

        // Generate new access token
        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());

        return AuthResponse.builder()
                .success(true)
                .accessToken(newAccessToken)
                .refreshToken(refreshTokenStr)
                .tokenType("Bearer")
                .expiresIn(jwtService.getAccessTokenExpiration())
                .user(toUserDto(user))
                .build();
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
        log.info("User logged out");
    }

    @Transactional
    public void logoutAll(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("User {} logged out from all devices", userId);
    }

    public Map<String, Object> forgotPassword(String email) {
        // Check if user exists
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            // Don't reveal if email exists for security
            return Map.of(
                    "success", true,
                    "message", "If the email exists, you will receive a password reset code"
            );
        }

        // Generate and send OTP
        otpService.generateAndSendOtp(email, OtpToken.OtpType.PASSWORD_RESET);

        return Map.of(
                "success", true,
                "message", "Password reset code sent to your email"
        );
    }

    public Map<String, Object> verifyOtp(String email, String otp) {
        boolean verified = otpService.verifyOtp(email, otp, OtpToken.OtpType.PASSWORD_RESET);
        
        if (verified) {
            return Map.of(
                    "success", true,
                    "message", "OTP verified successfully",
                    "verified", true
            );
        } else {
            return Map.of(
                    "success", false,
                    "error", "Invalid or expired OTP",
                    "verified", false
            );
        }
    }

    @Transactional
    public Map<String, Object> resetPassword(String email, String otp, String newPassword) {
        // Verify OTP first
        boolean verified = otpService.verifyOtp(email, otp, OtpToken.OtpType.PASSWORD_RESET);
        if (!verified) {
            return Map.of(
                    "success", false,
                    "error", "Invalid or expired OTP"
            );
        }

        // Find user
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            return Map.of(
                    "success", false,
                    "error", "User not found"
            );
        }

        User user = userOpt.get();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(Instant.now());
        userRepository.save(user);

        // Delete OTP
        otpService.deleteOtp(email, OtpToken.OtpType.PASSWORD_RESET);

        // Invalidate all refresh tokens for security
        refreshTokenRepository.deleteByUserId(user.getId());

        log.info("Password reset successful for user: {}", user.getUsername());

        return Map.of(
                "success", true,
                "message", "Password reset successful. Please login with your new password."
        );
    }

    public Map<String, Object> checkUsernameAvailability(String username) {
        // Validate format
        if (username == null || !username.matches("^[a-z0-9_]{3,30}$")) {
            return Map.of(
                    "available", false,
                    "error", "Invalid username format"
            );
        }

        boolean exists = userRepository.existsByUsername(username);

        if (!exists) {
            return Map.of("available", true);
        }

        // Generate suggestions
        List<String> suggestions = generateUsernameSuggestions(username);

        return Map.of(
                "available", false,
                "suggestions", suggestions
        );
    }

    public Optional<User> validateToken(String token) {
        if (!jwtService.isTokenValid(token)) {
            return Optional.empty();
        }

        String userId = jwtService.extractUserId(token);
        return userRepository.findById(userId);
    }

    private void saveRefreshToken(String userId, String token, String deviceInfo, String ipAddress) {
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .deviceInfo(deviceInfo)
                .ipAddress(ipAddress)
                .expiresAt(jwtService.getRefreshTokenExpiryInstant())
                .createdAt(Instant.now())
                .build();
        refreshTokenRepository.save(refreshToken);
    }

    private List<String> generateUsernameSuggestions(String username) {
        List<String> suggestions = new ArrayList<>();
        String baseName = username.replaceAll("[0-9_]+$", "");
        Random random = new Random();

        // Try numeric suffixes
        for (int i = 0; i < 10 && suggestions.size() < 3; i++) {
            String suggestion = baseName + random.nextInt(1000);
            if (!userRepository.existsByUsername(suggestion) && !suggestion.equals(username)) {
                suggestions.add(suggestion);
            }
        }

        // Try prefixes if needed
        String[] prefixes = {"the", "real", "im", "its", "hey"};
        for (String prefix : prefixes) {
            if (suggestions.size() >= 3) break;
            String suggestion = prefix + "_" + baseName;
            if (!userRepository.existsByUsername(suggestion)) {
                suggestions.add(suggestion);
            }
        }

        return suggestions.subList(0, Math.min(3, suggestions.size()));
    }

    private UserDto toUserDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .name(user.getName())
                .bio(user.getBio())
                .location(user.getLocation())
                .interests(user.getInterests())
                .avatarUrl(user.getAvatarUrl())
                .coverUrl(user.getCoverUrl())
                .followersCount(user.getFollowersCount())
                .followingCount(user.getFollowingCount())
                .postsCount(user.getPostsCount())
                .isPrivate(user.isPrivate())
                .isVerified(user.isVerified())
                .emailVerified(user.isEmailVerified())
                .lastActiveAt(user.getLastActiveAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}

