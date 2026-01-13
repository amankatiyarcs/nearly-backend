package com.nearly.messaging.model;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;

import java.time.Instant;

@Document(collection = "message_seen")
@CompoundIndex(name = "message_user_idx", def = "{'messageId': 1, 'userId': 1}", unique = true)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageSeen {
    
    @Id
    private String id;
    
    @Indexed
    private String messageId;
    
    @Indexed
    private String userId;
    
    private Instant seenAt;
}
