package com.reparaya.users.service;

import com.reparaya.users.dto.PresignUploadReq;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import com.sun.net.httpserver.HttpServer;
import java.util.Arrays;
import java.net.InetSocketAddress;
import java.net.URL;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class S3StorageServiceTest {

    @Mock
    private S3Presigner presigner;

    @Mock
    private PresignedPutObjectRequest presignedRequest;

    @Test
    void initWithoutCredentialsThrows() {
        S3StorageService service = new S3StorageService();
        assertThatThrownBy(service::init)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AWS credentials");
    }

    @Test
    void presignedURLUsesPresigner() throws Exception {
        S3StorageService service = new S3StorageService();
        ReflectionTestUtils.setField(service, "bucket", "my-bucket");
        ReflectionTestUtils.setField(service, "region", "us-east-1");
        ReflectionTestUtils.setField(service, "presigner", presigner);

        when(presigner.presignPutObject(any(java.util.function.Consumer.class)))
                .thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/upload"));

        PresignUploadReq req = new PresignUploadReq();
        req.setKey("k");
        req.setContentType("image/png");
        req.setExpiresIn(5);

        String url = service.presignedURL(req);

        assertThat(url).isEqualTo("https://example.com/upload");
    }

    @Test
    void generatePresignedUrlForUserBuildsKeyWithExtension() throws Exception {
        S3StorageService service = new S3StorageService();
        ReflectionTestUtils.setField(service, "bucket", "my-bucket");
        ReflectionTestUtils.setField(service, "region", "us-east-1");
        ReflectionTestUtils.setField(service, "presigner", presigner);

        when(presigner.presignPutObject(any(java.util.function.Consumer.class)))
                .thenReturn(presignedRequest);
        when(presignedRequest.url()).thenReturn(new URL("https://example.com/upload2"));

        S3StorageService.PresignedUrlResult result = service.generatePresignedUrlForUser(
                123L, "image/jpeg", "photo.jpg");

        assertThat(result.getKey()).startsWith("users/123/profile-").endsWith(".jpg");
        assertThat(result.getPresignedUrl()).isEqualTo("https://example.com/upload2");
    }

    @Test
    void uploadImageToS3_returnsPublicUrlOnSuccess() throws Exception {
        S3StorageService service = new S3StorageService();
        ReflectionTestUtils.setField(service, "bucket", "my-bucket");
        ReflectionTestUtils.setField(service, "region", "us-east-1");

        AtomicInteger calls = new AtomicInteger();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/upload", exchange -> {
            calls.incrementAndGet();
            exchange.getRequestBody().readAllBytes();
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();
        try {
            String presigned = "http://localhost:" + server.getAddress().getPort() + "/upload";
            byte[] bytes = "image-data".getBytes();

            String result = service.uploadImageToS3(presigned, bytes, "image/png", "key-123");

            assertThat(result).isEqualTo("https://my-bucket.s3.us-east-1.amazonaws.com/key-123");
            assertThat(calls.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void uploadImageToS3_throwsWhenResponseIsNot2xx() throws Exception {
        S3StorageService service = new S3StorageService();
        ReflectionTestUtils.setField(service, "bucket", "my-bucket");
        ReflectionTestUtils.setField(service, "region", "us-east-1");

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/upload", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        try {
            String presigned = "http://localhost:" + server.getAddress().getPort() + "/upload";

            assertThatThrownBy(() -> service.uploadImageToS3(presigned, "img".getBytes(), "image/png", "key"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("respuesta: 500");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadAndUploadImageFromUrl_throwsWhenContentTypeIsInvalid() throws Exception {
        S3StorageService service = new S3StorageService();
        ReflectionTestUtils.setField(service, "bucket", "bucket");
        ReflectionTestUtils.setField(service, "region", "us-east-1");

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/file", exchange -> {
            byte[] body = "not-image".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        String url = "http://localhost:" + server.getAddress().getPort() + "/file";
        try {
            assertThatThrownBy(() -> service.downloadAndUploadImageFromUrl(url, 1L))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Content-Type");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void downloadAndUploadImageFromUrl_uploadsWhenContentTypeIsImage() throws Exception {
        byte[] imageBytes = "png-data".getBytes();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/img/photo.png", exchange -> {
            exchange.getResponseHeaders().add("Content-Type", "image/png");
            exchange.sendResponseHeaders(200, imageBytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(imageBytes);
            }
        });
        server.start();

        S3StorageService service = spy(new S3StorageService());
        ReflectionTestUtils.setField(service, "bucket", "bucket");
        ReflectionTestUtils.setField(service, "region", "us-east-1");
        doReturn("http://presigned").when(service).presignedURL(any(PresignUploadReq.class));
        doReturn("https://public.example.com/users/1/profile.png")
                .when(service).uploadImageToS3(anyString(), any(byte[].class), anyString(), anyString());

        String url = "http://localhost:" + server.getAddress().getPort() + "/img/photo.png";
        try {
            String result = service.downloadAndUploadImageFromUrl(url, 1L);

            assertThat(result).isEqualTo("https://public.example.com/users/1/profile.png");
            verify(service).presignedURL(any(PresignUploadReq.class));
            verify(service).uploadImageToS3(
                    eq("http://presigned"),
                    argThat(arr -> Arrays.equals(arr, imageBytes)),
                    eq("image/png"),
                    contains("users/1/profile-")
            );
        } finally {
            server.stop(0);
        }
    }
}
