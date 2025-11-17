package com.reparaya.users.controller;

import com.reparaya.users.service.ProfileImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Slf4j
@Tag(name = "files-controller", description = "Operaciones de archivos (imagen de perfil)")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class FileController {

    private final ProfileImageService profileImageService;

    @Operation(summary = "Subir imagen de perfil")
    @PostMapping("/presign-upload")
    public ResponseEntity<?> presignUpload(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = profileImageService.uploadProfileImage(file);
            
            return ResponseEntity.ok(Map.of(
                    "imageUrl", imageUrl
            ));
        } catch (IllegalArgumentException e) {
            log.warn("Error de validación al subir imagen: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            log.warn("Error de autenticación al subir imagen: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Error al subir imagen: " + ex.getMessage()));
        }
    }
}
