package com.reparaya.users.service;

import com.reparaya.users.dto.UpdateUserResponse;
import com.reparaya.users.entity.User;
import com.reparaya.users.external.service.CorePublisherService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileImageServiceTest {

    @Mock private S3StorageService s3StorageService;
    @Mock private UserService userService;
    @Mock private CorePublisherService corePublisherService;
    @Mock private MultipartFile multipartFile;

    @InjectMocks
    private ProfileImageService profileImageService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void uploadProfileImage_happyPathUploadsAndUpdatesUser() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null)
        );

        User user = new User();
        user.setUserId(5L);
        user.setEmail("user@example.com");
        when(userService.getUserByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/png");
        when(multipartFile.getOriginalFilename()).thenReturn("photo.png");
        when(multipartFile.getBytes()).thenReturn(new byte[]{1, 2, 3});

        S3StorageService.PresignedUrlResult presigned = new S3StorageService.PresignedUrlResult("https://presigned", "key");
        when(s3StorageService.generatePresignedUrlForUser(anyLong(), anyString(), anyString())).thenReturn(presigned);
        when(s3StorageService.uploadImageToS3(anyString(), any(), anyString(), anyString()))
                .thenReturn("https://public/image.png");
        when(userService.updateUserProfileImage("user@example.com", "https://public/image.png")).thenReturn(user);
        when(userService.mapUserToDto(user)).thenReturn(null); // DTO value not relevant for this test

        String result = profileImageService.uploadProfileImage(multipartFile);

        assertThat(result).isEqualTo("https://public/image.png");
        verify(corePublisherService).sendUserUpdatedToCore(any(UpdateUserResponse.class));
    }

    @Test
    void uploadProfileImage_throwsWhenFileEmpty() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null)
        );
        when(multipartFile.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> profileImageService.uploadProfileImage(multipartFile))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void uploadProfileImage_throwsWhenUnauthenticated() {
        when(multipartFile.isEmpty()).thenReturn(false);
        when(multipartFile.getContentType()).thenReturn("image/png");

        assertThatThrownBy(() -> profileImageService.uploadProfileImage(multipartFile))
                .isInstanceOf(IllegalStateException.class);
    }
}
