package com.reparaya.users.service;

import com.reparaya.users.controller.FileController;
import com.reparaya.users.dto.PresignUploadReq;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class S3StorageService {

    AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
            "AKIAU6GDVJICARNAQWM6",
            "IUUHdqO8nAVUhejXXXJGFnRWVtaUi2TPJt0XWeFb"
    );

    private final S3Presigner presigner = S3Presigner.builder()
            .region(Region.US_EAST_2)
            .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
            .build();

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

    public String presignedURL(PresignUploadReq req) {
        UUID uuid = UUID.randomUUID();
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket("arreglaya-users-image-profile")
                .key(uuid.toString())
                .contentType(req.getContentType())
                .build();


        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(
                r -> r.signatureDuration(Duration.ofMinutes(10))
                        .putObjectRequest(put));

        return presignedRequest.url().toString();
    }
}
