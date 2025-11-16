package com.reparaya.users.service;

import com.reparaya.users.dto.AddPermissionRequest;
import com.reparaya.users.dto.RemovePermissionRequest;
import com.reparaya.users.entity.Permission;
import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import com.reparaya.users.repository.PermissionRepository;
import com.reparaya.users.repository.RoleRepository;
import com.reparaya.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@Transactional
class PermissionServiceIntegrationTest {

    @Autowired
    private PermissionService permissionService;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void cleanDatabase() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();
    }

    @Test
    void getPermissionsForUser_returnsPermissionsGroupedByModule() {
        Permission catalogView = permissionRepository.save(Permission.builder()
                .moduleCode("CATALOG")
                .permissionName("VIEW")
                .active(true)
                .build());
        Permission catalogEdit = permissionRepository.save(Permission.builder()
                .moduleCode("CATALOG")
                .permissionName("EDIT")
                .active(true)
                .build());
        Permission dashboardView = permissionRepository.save(Permission.builder()
                .moduleCode("DASHBOARD")
                .permissionName("VIEW")
                .active(true)
                .build());

        Role role = persistRole("ROLE_PROVIDER", catalogView, catalogEdit, dashboardView);
        User user = persistUser("provider@test.com", role, true);

        Map<String, List<String>> permissions = permissionService.getPermissionsForUser(user.getUserId());
        List<String> catalogPermissions = permissionService.getPermissionsForUserInModule(user.getUserId(), "CATALOG");

        assertThat(permissions).containsKeys("CATALOG", "DASHBOARD");
        assertThat(permissions.get("CATALOG")).containsExactlyInAnyOrder("VIEW", "EDIT");
        assertThat(catalogPermissions).containsExactlyInAnyOrder("VIEW", "EDIT");
        assertThat(permissionService.hasPermission(user.getUserId(), "CATALOG", "VIEW")).isTrue();
    }

    @Test
    void hasPermission_returnsFalseWhenUserIsInactive() {
        Permission permission = permissionRepository.save(Permission.builder()
                .moduleCode("CATALOG")
                .permissionName("DELETE")
                .active(true)
                .build());
        Role role = persistRole("ROLE_INACTIVE", permission);
        User user = persistUser("inactive@test.com", role, false);

        boolean hasPermission = permissionService.hasPermission(user.getUserId(), "CATALOG", "DELETE");

        assertThat(hasPermission).isFalse();
    }

    @Test
    void addAndRemovePermission_updatesRoleAssignments() {
        Permission manage = permissionRepository.save(Permission.builder()
                .moduleCode("CATALOG")
                .permissionName("MANAGE")
                .active(true)
                .build());
        Permission export = permissionRepository.save(Permission.builder()
                .moduleCode("CATALOG")
                .permissionName("EXPORT")
                .active(true)
                .build());
        Role role = persistRole("ROLE_ANALYST", manage);
        User user = persistUser("analyst@test.com", role, true);

        permissionService.addPermissionToUser(user.getUserId(), AddPermissionRequest.builder()
                .moduleCode("CATALOG")
                .permissionName("EXPORT")
                .build());

        User refreshed = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(refreshed.getRole().getPermissions())
                .extracting(Permission::getPermissionName)
                .containsExactlyInAnyOrder("MANAGE", "EXPORT");

        permissionService.removePermissionFromUser(user.getUserId(), RemovePermissionRequest.builder()
                .moduleCode("CATALOG")
                .permissionName("EXPORT")
                .build());

        User withoutExport = userRepository.findById(user.getUserId()).orElseThrow();
        assertThat(withoutExport.getRole().getPermissions())
                .extracting(Permission::getPermissionName)
                .containsExactly("MANAGE");
    }

    private Role persistRole(String roleName, Permission... permissions) {
        Set<Permission> permissionSet = new HashSet<>(Arrays.asList(permissions));
        Role role = Role.builder()
                .name(roleName)
                .description("integration role")
                .active(true)
                .permissions(permissionSet)
                .build();
        return roleRepository.save(role);
    }

    private User persistUser(String email, Role role, boolean active) {
        return userRepository.save(User.builder()
                .email(email)
                .firstName("Test")
                .lastName("User")
                .phoneNumber("555-1111")
                .dni(UUID.randomUUID().toString())
                .role(role)
                .active(active)
                .build());
    }
}
