package com.reparaya.users.service;

import com.reparaya.users.dto.PresignUploadReq;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
public class S3StorageService {

    @Value("${aws.s3.access-key}")
    private String accessKey;

    @Value("${aws.s3.secret-key}")
    private String secretKey;

    @Value("${aws.s3.bucket}")
    private String bucket;

    private final String region = "us-east-2";

    private S3Presigner presigner;
    private S3Client s3;

    @PostConstruct
    private void init() {
        try {
            AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, secretKey);

            this.presigner = S3Presigner.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .build();

            this.s3 = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                    .build();

            log.info("S3 client inicializado correctamente con región {}", region);
        } catch (Exception e) {
            log.error("Error inicializando S3StorageService: {}", e.getMessage());
            throw new IllegalStateException("No se pudo inicializar S3StorageService", e);
        }
    }

    public URL generateUploadPresignedUrl(String objectKey, String contentType, int minutes) {
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(minutes))
                .putObjectRequest(put)
                .build();

        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        return presigned.url();
    }

    public String buildPublicUrl(String objectKey) {
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + objectKey;
    }

    public String presignedURL(PresignUploadReq req) {
        UUID uuid = UUID.randomUUID();
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(uuid.toString())
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(put));

        return presignedRequest.url().toString();
    }
}
