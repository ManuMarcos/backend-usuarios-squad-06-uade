package com.reparaya.users.controller;

import com.reparaya.users.service.ProfileImageService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@org.junit.jupiter.api.extension.ExtendWith(MockitoExtension.class)
class FileControllerTest {

    @Mock private ProfileImageService profileImageService;
    @Mock private MultipartFile multipartFile;

    @InjectMocks
    private FileController fileController;

    @Test
    void presignUpload_returnsOkOnSuccess() {
        when(profileImageService.uploadProfileImage(any())).thenReturn("url");

        ResponseEntity<?> response = fileController.presignUpload(multipartFile);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("imageUrl", "url");
    }

    @Test
    void presignUpload_returnsBadRequestOnValidationError() {
        when(profileImageService.uploadProfileImage(any())).thenThrow(new IllegalArgumentException("bad"));

        ResponseEntity<?> response = fileController.presignUpload(multipartFile);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void presignUpload_returnsUnauthorizedOnAuthError() {
        when(profileImageService.uploadProfileImage(any())).thenThrow(new IllegalStateException("no auth"));

        ResponseEntity<?> response = fileController.presignUpload(multipartFile);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
    }

    @Test
    void presignUpload_returnsNotFoundOnResponseStatus() {
        when(profileImageService.uploadProfileImage(any()))
                .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "missing"));

        ResponseEntity<?> response = fileController.presignUpload(multipartFile);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
