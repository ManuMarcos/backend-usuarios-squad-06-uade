package com.reparaya.users.service;

import com.reparaya.users.entity.Permission;
import com.reparaya.users.entity.User;
import com.reparaya.users.repository.PermissionRepository;
import com.reparaya.users.repository.RoleRepository;
import com.reparaya.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

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

        log.info("Obtained permissions for user: {} (role: {}): {}",
                userId, user.getRole().getName(), permissionsByModule);
        return permissionsByModule;
    }

    public List<String> getPermissionsForUserInModule(Long userId, String moduleCode) {
        log.info("Obtaining permissions for user {} in module {}", userId, moduleCode);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        List<Permission> permissions = permissionRepository
                .findPermissionsByRoleIdAndModuleCode(user.getRole().getId(), moduleCode);

        List<String> permissionNames = permissions.stream()
                .map(Permission::getPermissionName)
                .collect(Collectors.toList());

        log.info("Permissions for user {} (role: {}) in module {}: {}",
                userId, user.getRole().getName(), moduleCode, permissionNames);
        return permissionNames;
    }

    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    public List<Permission> getPermissionsByModule(String moduleCode) {
        return permissionRepository.findPermissionsByModuleCode(moduleCode);
    }
}