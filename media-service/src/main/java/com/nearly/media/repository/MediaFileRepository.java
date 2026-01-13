package com.nearly.media.repository;

import com.nearly.media.model.MediaFile;
import com.nearly.media.model.MediaFile.MediaContext;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.Instant;
import java.util.List;

public interface MediaFileRepository extends MongoRepository<MediaFile, String> {
    List<MediaFile> findByUserId(String userId);
    List<MediaFile> findByContextAndContextId(MediaContext context, String contextId);
    List<MediaFile> findByExpiresAtBefore(Instant now);
    void deleteByExpiresAtBefore(Instant now);
}

