package com.nearly.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "otp_tokens")
public class OtpToken {
    @Id
    private String id;
    
    @Indexed
    private String email;
    
    private String otp;
    private OtpType type;
    private int attempts;
    private boolean verified;
    
    @Indexed(expireAfter = "0s")
    private Instant expiresAt;
    
    private Instant createdAt;
    
    public enum OtpType {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }
}

