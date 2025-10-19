package com.reparaya.users.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

@Tag(name = "files-controller", description = "Operaciones de archivos (imagen de perfil)")
@RestController
@RequestMapping("/api/users/files")
public class FileController {

    private final S3Presigner presigner;
    private final String bucket;

    public FileController(S3Presigner presigner,
                          @Value("${aws.s3.bucket}") String bucket) {
        this.presigner = presigner;
        this.bucket = bucket;
    }

    @Operation(summary = "Generar URL prefirmada para subir imagen")
    @PostMapping("/presign-upload")
    public ResponseEntity<?> presignUpload(@RequestBody PresignUploadReq req) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(req.key)
                .contentType(req.contentType)
                .build();

        PutObjectPresignRequest presignReq = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(req.expiresIn == null ? 300 : req.expiresIn))
                .putObjectRequest(put)
                .build();

        URL url = presigner.presignPutObject(presignReq).url();

        return ResponseEntity.ok(Map.of(
                "url", url.toString(),
                "headers", Map.of("Content-Type", req.contentType)
        ));
    }

    @Operation(summary = "Generar URL prefirmada para descargar imagen")
    @PostMapping("/presign-download")
    public ResponseEntity<?> presignDownload(@RequestBody PresignDownloadReq req) {
        GetObjectRequest get = GetObjectRequest.builder()
                .bucket(bucket)
                .key(req.key)
                .build();

        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(req.expiresIn == null ? 300 : req.expiresIn))
                .getObjectRequest(get)
                .build();

        URL url = presigner.presignGetObject(presignReq).url();

        return ResponseEntity.ok(Map.of("url", url.toString()));
    }

    @Data public static class PresignUploadReq {
        public String key;          // ej: users/2/profile.jpg
        public String contentType;  // ej: image/jpeg
        public Integer expiresIn;   // seg (opcional)
    }
    @Data public static class PresignDownloadReq {
        public String key;          // ej: users/2/profile.jpg
        public Integer expiresIn;   // seg (opcional)
    }
}
