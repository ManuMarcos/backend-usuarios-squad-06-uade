package com.reparaya.users.controller;

import com.reparaya.users.dto.*;
import com.reparaya.users.service.PermissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@Tag(name = "permission-controller", description = "Gestión de permisos por módulos")
@RestController
@RequestMapping("/api/permissions")
@RequiredArgsConstructor
@Slf4j
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    private final PermissionService permissionService;

    @Operation(
        summary = "Obtener permisos de un usuario",
        description = "Retorna todos los permisos de un usuario agrupados por módulo",
        parameters = {
            @Parameter(name = "userId", description = "ID del usuario", example = "1")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Permisos obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        }
    )
    @GetMapping("/user/{userId}")
    public ResponseEntity<Map<String, List<String>>> getUserPermissions(@PathVariable Long userId) {
        try {
            Map<String, List<String>> permissions = permissionService.getPermissionsForUser(userId);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error obteniendo permisos del usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Obtener permisos de un usuario en un módulo específico",
        description = "Retorna los permisos de un usuario para un módulo específico",
        parameters = {
            @Parameter(name = "userId", description = "ID del usuario", example = "1"),
            @Parameter(name = "moduleCode", description = "Código del módulo", example = "module_catalog")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Permisos obtenidos exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        }
    )
    @GetMapping("/user/{userId}/module/{moduleCode}")
    public ResponseEntity<List<String>> getUserPermissionsInModule(
            @PathVariable Long userId, 
            @PathVariable String moduleCode) {
        try {
            List<String> permissions = permissionService.getPermissionsForUserInModule(userId, moduleCode);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error obteniendo permisos del usuario {} en módulo {}: {}", userId, moduleCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Verificar si un usuario tiene un permiso específico",
        description = "Verifica si un usuario tiene un permiso específico en un módulo",
        parameters = {
            @Parameter(name = "userId", description = "ID del usuario", example = "1"),
            @Parameter(name = "moduleCode", description = "Código del módulo", example = "module_catalog"),
            @Parameter(name = "permission", description = "Nombre del permiso", example = "ZONA_VER")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Verificación completada"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        }
    )
    @GetMapping("/user/{userId}/module/{moduleCode}/permission/{permission}")
    public ResponseEntity<Boolean> hasPermission(
            @PathVariable Long userId,
            @PathVariable String moduleCode,
            @PathVariable String permission) {
        try {
            boolean hasPermission = permissionService.hasPermission(userId, moduleCode, permission);
            return ResponseEntity.ok(hasPermission);
        } catch (Exception e) {
            log.error("Error verificando permiso del usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Obtener todos los permisos disponibles",
        description = "Retorna todos los permisos disponibles en el sistema",
        responses = {
            @ApiResponse(responseCode = "200", description = "Permisos obtenidos exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        }
    )
    @GetMapping("/all")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        try {
            List<PermissionDto> permissions = permissionService.getAllPermissionsDto();
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error obteniendo todos los permisos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(
        summary = "Obtener permisos por módulo",
        description = "Retorna todos los permisos disponibles para un módulo específico",
        parameters = {
            @Parameter(name = "moduleCode", description = "Código del módulo", example = "module_catalog")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Permisos obtenidos exitosamente"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        }
    )
    @GetMapping("/module/{moduleCode}")
    public ResponseEntity<List<PermissionDto>> getPermissionsByModule(@PathVariable String moduleCode) {
        try {
            List<PermissionDto> permissions = permissionService.getPermissionsByModuleDto(moduleCode);
            return ResponseEntity.ok(permissions);
        } catch (Exception e) {
            log.error("Error obteniendo permisos del módulo {}: {}", moduleCode, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // =====================================================
    // ENDPOINTS PARA GESTIONAR PERMISOS DE USUARIOS
    // =====================================================

    @Operation(
        summary = "Agregar permiso a usuario",
        description = "Agrega un permiso específico a un usuario (solo ADMIN)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Permiso agregado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario o permiso no encontrado"),
            @ApiResponse(responseCode = "409", description = "El usuario ya tiene este permiso"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        }
    )
    @PostMapping("/user/{userId}/add")
    public ResponseEntity<String> addPermissionToUser(
            @PathVariable Long userId,
            @Valid @RequestBody AddPermissionRequest request) {
        try {
            permissionService.addPermissionToUser(userId, request);
            return ResponseEntity.ok("Permiso agregado exitosamente");
        } catch (IllegalArgumentException e) {
            log.warn("Error agregando permiso al usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (IllegalStateException e) {
            log.warn("Error agregando permiso al usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error agregando permiso al usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor");
        }
    }

    @Operation(
        summary = "Quitar permiso de usuario",
        description = "Quita un permiso específico de un usuario (solo ADMIN)",
        responses = {
            @ApiResponse(responseCode = "200", description = "Permiso quitado exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario o permiso no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        }
    )
    @DeleteMapping("/user/{userId}/remove")
    public ResponseEntity<String> removePermissionFromUser(
            @PathVariable Long userId,
            @Valid @RequestBody RemovePermissionRequest request) {
        try {
            permissionService.removePermissionFromUser(userId, request);
            return ResponseEntity.ok("Permiso quitado exitosamente");
        } catch (IllegalArgumentException e) {
            log.warn("Error quitando permiso del usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error quitando permiso del usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor");
        }
    }

    @Operation(
        summary = "Sincronizar permisos de usuario con su rol",
        description = "Sincroniza los permisos de un usuario con los permisos de su rol actual (solo ADMIN)",
        parameters = {
            @Parameter(name = "userId", description = "ID del usuario", example = "1")
        },
        responses = {
            @ApiResponse(responseCode = "200", description = "Permisos sincronizados exitosamente"),
            @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor")
        }
    )
    @PostMapping("/user/{userId}/sync")
    public ResponseEntity<String> syncUserPermissionsWithRole(@PathVariable Long userId) {
        try {
            permissionService.syncUserPermissionsWithRole(userId);
            return ResponseEntity.ok("Permisos sincronizados exitosamente");
        } catch (IllegalArgumentException e) {
            log.warn("Error sincronizando permisos del usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            log.error("Error sincronizando permisos del usuario {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno del servidor");
        }
    }
}
