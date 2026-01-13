package com.nearly.media.controller;

import com.nearly.media.dto.*;
import com.nearly.media.model.MediaFile;
import com.nearly.media.model.MediaFile.MediaContext;
import com.nearly.media.service.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
public class MediaController {
    
    private final MediaService mediaService;

    /**
     * Direct file upload - for smaller files
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("userId") String userId,
            @RequestParam(value = "context", defaultValue = "OTHER") MediaContext context,
            @RequestParam(value = "contextId", required = false) String contextId,
            @RequestParam(value = "isPublic", defaultValue = "true") boolean isPublic) {


        UploadRequest request = UploadRequest.builder()
            .userId(userId)
            .context(context)
            .contextId(contextId)
            .isPublic(isPublic)
            .build();
        
        UploadResponse response = mediaService.uploadFile(file, request);
        
        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Multiple file upload
     */
    @PostMapping(value = "/upload/multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<UploadResponse>> uploadMultipleFiles(
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("userId") String userId,
            @RequestParam(value = "context", defaultValue = "OTHER") MediaContext context,
            @RequestParam(value = "contextId", required = false) String contextId,
            @RequestParam(value = "isPublic", defaultValue = "true") boolean isPublic) {
        
        UploadRequest request = UploadRequest.builder()
            .userId(userId)
            .context(context)
            .contextId(contextId)
            .isPublic(isPublic)
            .build();
        
        List<UploadResponse> responses = java.util.Arrays.stream(files)
            .map(file -> mediaService.uploadFile(file, request))
            .toList();
        
        return ResponseEntity.ok(responses);
    }

    /**
     * Get presigned URL for direct client-side upload (for larger files)
     */
    @PostMapping("/presigned-url")
    public ResponseEntity<PresignedUrlResponse> getPresignedUrl(@RequestBody PresignedUrlRequest request) {
        return ResponseEntity.ok(mediaService.getPresignedUploadUrl(request));
    }

    /**
     * Confirm upload completed (for presigned URL uploads)
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Void> confirmUpload(
            @PathVariable String id,
            @RequestBody Map<String, Long> body) {
        mediaService.confirmUpload(id, body.getOrDefault("fileSize", 0L));
        return ResponseEntity.ok().build();
    }

    /**
     * Get media file by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<MediaFile> getMediaFile(@PathVariable String id) {
        return mediaService.getMediaFile(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get media files by context (e.g., all images for an activity)
     */
    @GetMapping("/context/{context}/{contextId}")
    public ResponseEntity<List<MediaFile>> getMediaByContext(
            @PathVariable MediaContext context,
            @PathVariable String contextId) {
        return ResponseEntity.ok(mediaService.getMediaByContext(context, contextId));
    }

    /**
     * Get all media files for a user
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<MediaFile>> getMediaByUser(@PathVariable String userId) {
        return ResponseEntity.ok(mediaService.getMediaByUser(userId));
    }

    /**
     * Delete media file
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMedia(@PathVariable String id) {
        mediaService.deleteMedia(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}

