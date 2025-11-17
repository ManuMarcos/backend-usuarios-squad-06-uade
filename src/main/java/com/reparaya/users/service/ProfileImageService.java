package com.reparaya.users.service;

import com.reparaya.users.dto.UpdateUserResponse;
import com.reparaya.users.external.service.CorePublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;

import static com.reparaya.users.service.UserService.ADMIN_ROLE;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProfileImageService {

    private final S3StorageService s3StorageService;
    private final UserService userService;
    private final CorePublisherService corePublisherService;

    @Transactional
    public String uploadProfileImage(MultipartFile file) {
        // Validar archivo
        validateImageFile(file);

        // Obtener usuario autenticado
        String email = getAuthenticatedUserEmail();

        var userOpt = userService.getUserByEmail(email);
        
        if (userOpt.isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404),"Usuario no encontrado con email: " + email);
        }

        Long userId = userOpt.get().getUserId();
        String contentType = file.getContentType();

        // Generar URL presigned
        var presignedResult = s3StorageService.generatePresignedUrlForUser(
                userId, 
                contentType, 
                file.getOriginalFilename()
        );
        
        log.info("URL presigned generada para usuario {}: {}", email, presignedResult.getPresignedUrl());

        // Subir la imagen a S3
        byte[] imageBytes;
        try {
            imageBytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("Error al leer el archivo: " + e.getMessage(), e);
        }

        String imageUrl = s3StorageService.uploadImageToS3(
                presignedResult.getPresignedUrl(), 
                imageBytes, 
                contentType, 
                presignedResult.getKey()
        );
        
        log.info("Imagen subida exitosamente a S3: {}", imageUrl);

        // Actualizar el usuario con la URL de la imagen
        var updatedUser = userService.updateUserProfileImage(email, imageUrl);

        var responseToCore = UpdateUserResponse.builder()
                .zones(new ArrayList<>())
                .skills(new ArrayList<>())
                .user(userService.mapUserToDto(updatedUser))
                .build();


        corePublisherService.sendUserUpdatedToCore(responseToCore);

        return imageUrl;
    }

    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen");
        }
    }

    private String getAuthenticatedUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }

        return authentication.getPrincipal().toString();
    }
}

