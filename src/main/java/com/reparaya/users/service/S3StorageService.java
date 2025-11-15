package com.reparaya.users.service;

import com.reparaya.users.dto.PresignUploadReq;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import jakarta.annotation.PostConstruct;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3StorageService {



    @Value("${aws.s3.accessKey}")
    private String accessKey;

    @Value("${aws.s3.secretKey}")
    private String secretKey;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    private S3Presigner presigner;

    @PostConstruct
    void init() {
        if (accessKey == null || accessKey.isBlank() || secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("AWS credentials not configured.");
        }

        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

        this.presigner = S3Presigner.builder()
                .region(Region.US_EAST_2) 
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    public String presignedURL(PresignUploadReq req) {
        UUID uuid = UUID.randomUUID();
        String key = req.getKey() != null ? req.getKey() : uuid.toString();
        
        PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key);
        
        if (req.getContentType() != null) {
            putBuilder.contentType(req.getContentType());
        }
        
        PutObjectRequest put = putBuilder.build();

        Duration expiration = req.getExpiresIn() != null 
                ? Duration.ofMinutes(req.getExpiresIn()) 
                : Duration.ofMinutes(10);

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.signatureDuration(expiration)
                        .putObjectRequest(put));

        return presignedRequest.url().toString();
    }
    
    public String uploadImageToS3(String presignedUrl, byte[] imageBytes, String contentType, String key) {
        try {
            URI uri = URI.create(presignedUrl);
            java.net.URL url = uri.toURL();
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setRequestMethod("PUT");
            connection.setRequestProperty("Content-Type", contentType);
            
            try (java.io.OutputStream os = connection.getOutputStream()) {
                os.write(imageBytes);
            }
            
            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                // Construir la URL pública del objeto
                return String.format("https://%s.s3.%s.amazonaws.com/%s", bucket, region, key);
            } else {
                throw new RuntimeException("Error al subir imagen a S3. Código de respuesta: " + responseCode);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error al subir imagen a S3: " + e.getMessage(), e);
        }
    }
    
    public PresignedUrlResult generatePresignedUrlForUser(Long userId, String contentType, String originalFilename) {
        // Extraer extensión del archivo original
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        } else {
            // Si no hay extensión, inferirla del content type
            if (contentType != null) {
                if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                    extension = ".jpg";
                } else if (contentType.contains("png")) {
                    extension = ".png";
                } else if (contentType.contains("gif")) {
                    extension = ".gif";
                } else if (contentType.contains("webp")) {
                    extension = ".webp";
                } else {
                    extension = ".jpg"; // default
                }
            }
        }
        
        String key = String.format("users/%d/profile-%s%s", userId, UUID.randomUUID(), extension);
        PresignUploadReq req = new PresignUploadReq();
        req.setKey(key);
        req.setContentType(contentType);
        req.setExpiresIn(10);
        String presignedUrl = presignedURL(req);
        return new PresignedUrlResult(presignedUrl, key);
    }
    
    public String downloadAndUploadImageFromUrl(String imageUrl, Long userId) {
        try {
            // Descargar la imagen desde la URL
            URI uri = URI.create(imageUrl);
            java.net.URL url = uri.toURL();
            java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000); // 10 segundos
            connection.setReadTimeout(10000);
            
            // Obtener content type de la respuesta
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IllegalArgumentException("La URL no apunta a una imagen válida. Content-Type: " + contentType);
            }
            
            // Leer los bytes de la imagen
            byte[] imageBytes;
            try (java.io.InputStream inputStream = connection.getInputStream();
                 java.io.ByteArrayOutputStream outputStream = new java.io.ByteArrayOutputStream()) {
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                imageBytes = outputStream.toByteArray();
            }
            
            // Extraer extensión de la URL o inferirla del content type
            String extension = extractExtensionFromUrl(imageUrl, contentType);
            String key = String.format("users/%d/profile-%s%s", userId, UUID.randomUUID(), extension);
            
            // Generar URL presigned
            PresignUploadReq req = new PresignUploadReq();
            req.setKey(key);
            req.setContentType(contentType);
            req.setExpiresIn(10);
            String presignedUrl = presignedURL(req);
            
            // Subir a S3
            return uploadImageToS3(presignedUrl, imageBytes, contentType, key);
            
        } catch (Exception e) {
            throw new RuntimeException("Error al descargar y subir imagen desde URL: " + e.getMessage(), e);
        }
    }

    private String extractExtensionFromUrl(String url, String contentType) {
        try {
            // Intentar extraer extensión de la URL
            String path = URI.create(url).getPath();
            if (path != null && path.contains(".")) {
                String ext = path.substring(path.lastIndexOf("."));
                if (ext.length() <= 5) { // Extensiones válidas son cortas
                    return ext.toLowerCase();
                }
            }
        } catch (Exception e) {
            // Si falla, continuar con inferencia del content type
        }
        
        // Inferir extensión del content type
        if (contentType != null) {
            if (contentType.contains("jpeg") || contentType.contains("jpg")) {
                return ".jpg";
            } else if (contentType.contains("png")) {
                return ".png";
            } else if (contentType.contains("gif")) {
                return ".gif";
            } else if (contentType.contains("webp")) {
                return ".webp";
            }
        }
        return ".jpg"; // default
    }
    
    public static class PresignedUrlResult {
        private final String presignedUrl;
        private final String key;
        
        public PresignedUrlResult(String presignedUrl, String key) {
            this.presignedUrl = presignedUrl;
            this.key = key;
        }
        
        public String getPresignedUrl() {
            return presignedUrl;
        }
        
        public String getKey() {
            return key;
        }
    }
}
