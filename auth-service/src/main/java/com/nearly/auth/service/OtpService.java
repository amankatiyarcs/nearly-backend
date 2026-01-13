package com.nearly.auth.service;

import com.nearly.auth.model.OtpToken;
import com.nearly.auth.repository.OtpTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OtpService {

    private final OtpTokenRepository otpTokenRepository;
    private final EmailService emailService;
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int MAX_ATTEMPTS = 5;

    @Value("${otp.expiration}")
    private Long otpExpiration;

    @Value("${otp.length}")
    private Integer otpLength;

    public String generateAndSendOtp(String email, OtpToken.OtpType type) {
        // Delete any existing OTP for this email and type
        otpTokenRepository.deleteByEmailAndType(email, type);
        
        // Generate new OTP
        String otp = generateOtp();
        
        // Save OTP token
        OtpToken otpToken = OtpToken.builder()
                .email(email)
                .otp(otp)
                .type(type)
                .attempts(0)
                .verified(false)
                .expiresAt(Instant.now().plusMillis(otpExpiration))
                .createdAt(Instant.now())
                .build();
        
        otpTokenRepository.save(otpToken);
        
        // Send email
        String purpose = type == OtpToken.OtpType.PASSWORD_RESET ? "PASSWORD_RESET" : "EMAIL_VERIFICATION";
        emailService.sendOtpEmail(email, otp, purpose);
        
        log.info("OTP generated and sent to {} for {}", email, type);
        return otp;
    }

    public boolean verifyOtp(String email, String otp, OtpToken.OtpType type) {
        Optional<OtpToken> otpTokenOpt = otpTokenRepository
                .findByEmailAndTypeAndVerifiedFalse(email, type);
        
        if (otpTokenOpt.isEmpty()) {
            log.warn("No OTP found for email: {} and type: {}", email, type);
            return false;
        }
        
        OtpToken otpToken = otpTokenOpt.get();
        
        // Check if expired
        if (otpToken.getExpiresAt().isBefore(Instant.now())) {
            log.warn("OTP expired for email: {}", email);
            otpTokenRepository.delete(otpToken);
            return false;
        }
        
        // Check max attempts
        if (otpToken.getAttempts() >= MAX_ATTEMPTS) {
            log.warn("Max OTP attempts exceeded for email: {}", email);
            otpTokenRepository.delete(otpToken);
            return false;
        }
        
        // Increment attempts
        otpToken.setAttempts(otpToken.getAttempts() + 1);
        
        // Verify OTP
        if (!otpToken.getOtp().equals(otp)) {
            otpTokenRepository.save(otpToken);
            log.warn("Invalid OTP for email: {}. Attempt {}/{}", email, otpToken.getAttempts(), MAX_ATTEMPTS);
            return false;
        }
        
        // Mark as verified
        otpToken.setVerified(true);
        otpTokenRepository.save(otpToken);
        
        log.info("OTP verified successfully for email: {}", email);
        return true;
    }

    public boolean isOtpVerified(String email, OtpToken.OtpType type) {
        return otpTokenRepository.findByEmailAndTypeAndVerifiedFalse(email, type)
                .map(OtpToken::isVerified)
                .orElse(false);
    }

    public void deleteOtp(String email, OtpToken.OtpType type) {
        otpTokenRepository.deleteByEmailAndType(email, type);
    }

    private String generateOtp() {
        StringBuilder otp = new StringBuilder();
        for (int i = 0; i < otpLength; i++) {
            otp.append(SECURE_RANDOM.nextInt(10));
        }
        return otp.toString();
    }
}

