package com.reparaya.users.service;

import com.reparaya.users.dto.*;
import com.reparaya.users.entity.Permission;
import com.reparaya.users.entity.User;
import com.reparaya.users.repository.PermissionRepository;
import com.reparaya.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public Map<String, List<String>> getPermissionsForUser(Long userId) {
        log.info("Obtaining user permissions for user id: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Obtener todos los permisos del rol del usuario, agrupados por módulo
        List<Permission> permissions = permissionRepository.findPermissionsByRoleId(user.getRole().getId());

        // Agrupar permisos por módulo
        Map<String, List<String>> permissionsByModule = permissions.stream()
                .collect(Collectors.groupingBy(
                        Permission::getModuleCode,
                        Collectors.mapping(Permission::getPermissionName, Collectors.toList())
                ));

        return permissionsByModule;
    }

    public List<String> getPermissionsForUserInModule(Long userId, String moduleCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<Permission> permissions = permissionRepository
                .findPermissionsByRoleIdAndModuleCode(user.getRole().getId(), moduleCode);

        List<String> permissionNames = permissions.stream()
                .map(Permission::getPermissionName)
                .collect(Collectors.toList());

        return permissionNames;
    }

    public boolean hasPermission(Long userId, String moduleCode, String permission) {
        try {
            User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado: " + userId));
            
            if (!user.getActive()) {
                return false;
            }
            
            List<Permission> permissions = permissionRepository
                .findPermissionsByRoleIdAndModuleCode(user.getRole().getId(), moduleCode);
            
            return permissions.stream()
                .anyMatch(p -> p.getPermissionName().equals(permission) && p.isActive());
                
        } catch (Exception e) {
            log.error("Error validando permiso: {}", e.getMessage());
            return false;
        }
    }
    
    public List<PermissionDto> getAllPermissionsDto() {
        List<Permission> permissions = permissionRepository.findAll();
        return permissions.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }
    
    public List<PermissionDto> getPermissionsByModuleDto(String moduleCode) {
        List<Permission> permissions = permissionRepository.findPermissionsByModuleCode(moduleCode);
        return permissions.stream()
            .map(this::mapToDto)
            .collect(Collectors.toList());
    }

    @Transactional
    public void addPermissionToUser(Long userId, AddPermissionRequest request) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        
        Permission permission = permissionRepository.findAll().stream()
            .filter(p -> p.getModuleCode().equals(request.getModuleCode()) 
                      && p.getPermissionName().equals(request.getPermissionName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Permiso no encontrado"));
        
        // Verificar si ya tiene el permiso
        boolean alreadyHasPermission = user.getRole().getPermissions().contains(permission);
        if (alreadyHasPermission) {
            throw new IllegalStateException("El usuario ya tiene este permiso");
        }
        
        // Agregar permiso al rol del usuario
        user.getRole().getPermissions().add(permission);
        userRepository.save(user);
        
        log.info("Permiso agregado exitosamente al usuario {}", userId);
    }
    
    @Transactional
    public void removePermissionFromUser(Long userId, RemovePermissionRequest request) {

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        
        Permission permission = permissionRepository.findAll().stream()
            .filter(p -> p.getModuleCode().equals(request.getModuleCode()) 
                      && p.getPermissionName().equals(request.getPermissionName()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Permiso no encontrado"));
        
        // Quitar permiso del rol del usuario
        user.getRole().getPermissions().remove(permission);
        userRepository.save(user);
        
        log.info("Permiso quitado exitosamente del usuario {}", userId);
    }
    
    @Transactional
    public void syncUserPermissionsWithRole(Long userId) {
        log.info("Sincronizando permisos del usuario {} con su rol", userId);
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado: " + userId));
        
        // Los permisos ya están sincronizados automáticamente con el rol
        // Este método es más para logging y validación
        log.info("Permisos del usuario {} sincronizados con rol {}", userId, user.getRole().getName());
    }
    
    private PermissionDto mapToDto(Permission permission) {
        return PermissionDto.builder()
            .id(permission.getId())
            .moduleCode(permission.getModuleCode())
            .permissionName(permission.getPermissionName())
            .description(permission.getDescription())
            .active(permission.isActive())
            .build();
    }
}