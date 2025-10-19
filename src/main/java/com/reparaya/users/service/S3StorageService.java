package com.reparaya.users.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.net.URL;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Presigner presigner;
    private final S3Client s3;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

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
}
