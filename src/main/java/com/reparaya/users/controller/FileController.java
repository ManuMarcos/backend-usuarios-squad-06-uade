package com.reparaya.users.controller;

import com.reparaya.users.dto.PresignUploadReq;
import com.reparaya.users.service.S3StorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import java.util.Map;

@Tag(name = "files-controller", description = "Operaciones de archivos (imagen de perfil)")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final S3Presigner presigner;
    private final S3StorageService S3StorageService;


    @Operation(summary = "Generar URL prefirmada para subir imagen")
    @PostMapping("/presign-upload")
    public ResponseEntity<?> presignUpload(@RequestBody PresignUploadReq req) {

        String url = S3StorageService.presignedURL(req);

        return ResponseEntity.ok(Map.of(
                "url", url,
                "headers", Map.of("Content-Type", req.getContentType())
        ));
    }

//    @Operation(summary = "Generar URL prefirmada para descargar imagen")
//    @PostMapping("/presign-download")
//    public ResponseEntity<?> presignDownload(@RequestBody PresignDownloadReq req) {
//        GetObjectRequest get = GetObjectRequest.builder()
//                .bucket(bucket)
//                .key(req.key)
//                .build();
//
//        GetObjectPresignRequest presignReq = GetObjectPresignRequest.builder()
//                .signatureDuration(Duration.ofSeconds(req.expiresIn == null ? 300 : req.expiresIn))
//                .getObjectRequest(get)
//                .build();
//
//        URL url = presigner.presignGetObject(presignReq).url();
//
//        return ResponseEntity.ok(Map.of("url", url.toString()));
//    }

//    @Data public static class PresignUploadReq {
//        public String key;          // ej: users/2/profile.jpg
//        public String contentType;  // ej: image/jpeg
//        public Integer expiresIn;   // seg (opcional)
//    }
//
//    @Data public static class PresignDownloadReq {
//        public String key;          // ej: users/2/profile.jpg
//        public Integer expiresIn;   // seg (opcional)
//    }
}
