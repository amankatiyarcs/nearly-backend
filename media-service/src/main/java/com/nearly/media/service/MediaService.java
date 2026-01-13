package com.nearly.media.service;

import com.nearly.media.config.S3Config;
import com.nearly.media.dto.*;
import com.nearly.media.model.MediaFile;
import com.nearly.media.model.MediaFile.MediaContext;
import com.nearly.media.model.MediaFile.MediaType;
import com.nearly.media.repository.MediaFileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class MediaService {
    
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Config s3Config;
    private final MediaFileRepository repository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final Set<String> IMAGE_TYPES = Set.of("image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> VIDEO_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");
    private static final Set<String> AUDIO_TYPES = Set.of("audio/mpeg", "audio/wav", "audio/ogg");
    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    public UploadResponse uploadFile(MultipartFile file, UploadRequest request) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return UploadResponse.builder().success(false).error("File is empty").build();
            }
            if (file.getSize() > MAX_FILE_SIZE) {
                return UploadResponse.builder().success(false).error("File too large. Max 100MB").build();
            }

            String contentType = file.getContentType();
            MediaType mediaType = determineMediaType(contentType);
            
            // Generate unique file name
            String extension = getFileExtension(file.getOriginalFilename());
            String fileName = UUID.randomUUID().toString() + extension;
            String s3Key = buildS3Key(request.getContext(), request.getContextId(), fileName);

            // Upload to S3
            PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(s3Config.getBucketName())
                .key(s3Key)
                .contentType(contentType)
                .contentLength(file.getSize())
                .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

            // Build public URL
            String publicUrl = buildPublicUrl(s3Key);

            // Save metadata
            MediaFile mediaFile = MediaFile.builder()
                .userId(request.getUserId())
                .fileName(fileName)
                .originalFileName(file.getOriginalFilename())
                .contentType(contentType)
                .fileSize(file.getSize())
                .s3Key(s3Key)
                .url(publicUrl)
                .mediaType(mediaType)
                .context(request.getContext())
                .contextId(request.getContextId())
                .isPublic(request.isPublic())
                .createdAt(Instant.now())
                .build();

            MediaFile saved = repository.save(mediaFile);

            // Publish event
            kafkaTemplate.send("media-events", Map.of(
                "type", "MEDIA_UPLOADED",
                "mediaId", saved.getId(),
                "userId", request.getUserId(),
                "context", request.getContext().name(),
                "url", publicUrl
            ));

            log.info("Uploaded file: {} for user: {}", fileName, request.getUserId());

            return UploadResponse.builder()
                .id(saved.getId())
                .url(publicUrl)
                .fileName(fileName)
                .contentType(contentType)
                .fileSize(file.getSize())
                .mediaType(mediaType)
                .success(true)
                .build();

        } catch (IOException e) {
            log.error("Failed to upload file", e);
            return UploadResponse.builder().success(false).error("Upload failed: " + e.getMessage()).build();
        }
    }

    public PresignedUrlResponse getPresignedUploadUrl(PresignedUrlRequest request) {
        String extension = getFileExtension(request.getFileName());
        String fileName = UUID.randomUUID().toString() + extension;
        String s3Key = buildS3Key(request.getContext(), request.getContextId(), fileName);

        // Create presigned URL for direct upload
        PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(s3Config.getBucketName())
            .key(s3Key)
            .contentType(request.getContentType())
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofMinutes(15))
            .putObjectRequest(putRequest)
            .build();

        var presignedUrl = s3Presigner.presignPutObject(presignRequest);

        // Pre-create the media file record
        MediaFile mediaFile = MediaFile.builder()
            .userId(request.getUserId())
            .fileName(fileName)
            .originalFileName(request.getFileName())
            .contentType(request.getContentType())
            .s3Key(s3Key)
            .url(buildPublicUrl(s3Key))
            .mediaType(determineMediaType(request.getContentType()))
            .context(request.getContext())
            .contextId(request.getContextId())
            .isPublic(request.isPublic())
            .createdAt(Instant.now())
            .build();

        MediaFile saved = repository.save(mediaFile);

        return PresignedUrlResponse.builder()
            .uploadUrl(presignedUrl.url().toString())
            .fileId(saved.getId())
            .s3Key(s3Key)
            .publicUrl(buildPublicUrl(s3Key))
            .expiresIn(900) // 15 minutes
            .build();
    }

    public void confirmUpload(String fileId, long fileSize) {
        repository.findById(fileId).ifPresent(file -> {
            file.setFileSize(fileSize);
            repository.save(file);
            log.info("Confirmed upload for file: {}", fileId);
        });
    }

    public Optional<MediaFile> getMediaFile(String id) {
        return repository.findById(id);
    }

    public List<MediaFile> getMediaByContext(MediaContext context, String contextId) {
        return repository.findByContextAndContextId(context, contextId);
    }

    public List<MediaFile> getMediaByUser(String userId) {
        return repository.findByUserId(userId);
    }

    public void deleteMedia(String id) {
        repository.findById(id).ifPresent(file -> {
            try {
                // Delete from S3
                DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(file.getS3Key())
                    .build();
                s3Client.deleteObject(deleteRequest);
                
                // Delete from database
                repository.delete(file);
                
                log.info("Deleted media file: {}", id);
            } catch (Exception e) {
                log.error("Failed to delete file from S3: {}", file.getS3Key(), e);
            }
        });
    }

    @Scheduled(cron = "0 0 3 * * *") // Every day at 3 AM
    public void cleanupExpiredMedia() {
        repository.findByExpiresAtBefore(Instant.now()).forEach(file -> {
            deleteMedia(file.getId());
        });
    }

    private String buildS3Key(MediaContext context, String contextId, String fileName) {
        String folder = context != null ? context.name().toLowerCase() : "other";
        if (contextId != null && !contextId.isEmpty()) {
            return String.format("%s/%s/%s", folder, contextId, fileName);
        }
        return String.format("%s/%s", folder, fileName);
    }

    private String buildPublicUrl(String s3Key) {
        // For Supabase S3 storage
        return s3Config.getEndpoint().replace("/s3", "/object/public/" + s3Config.getBucketName() + "/" + s3Key);
    }

    private MediaType determineMediaType(String contentType) {
        if (contentType == null) return MediaType.DOCUMENT;
        if (IMAGE_TYPES.contains(contentType)) return MediaType.IMAGE;
        if (VIDEO_TYPES.contains(contentType)) return MediaType.VIDEO;
        if (AUDIO_TYPES.contains(contentType)) return MediaType.AUDIO;
        return MediaType.DOCUMENT;
    }

    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf("."));
    }
}

