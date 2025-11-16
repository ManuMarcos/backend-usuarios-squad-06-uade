package com.reparaya.users.controller;

import com.reparaya.users.dto.AddPermissionRequest;
import com.reparaya.users.dto.RemovePermissionRequest;
import com.reparaya.users.entity.Permission;
import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import com.reparaya.users.repository.PermissionRepository;
import com.reparaya.users.repository.RoleRepository;
import com.reparaya.users.repository.UserRepository;
import com.reparaya.users.util.RegisterOriginEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PermissionControllerIntegrationTest {

    @Autowired
    private PermissionController permissionController;

    @Autowired
    private PermissionRepository permissionRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    private Role adminRole;
    private User adminUser;
    private Permission basePermission;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
        permissionRepository.deleteAll();

        basePermission = permissionRepository.save(
                Permission.builder()
                        .moduleCode("catalogue")
                        .permissionName("VIEW")
                        .active(true)
                        .build());

        adminRole = roleRepository.save(
                Role.builder()
                        .name("ROLE_ADMIN")
                        .description("Administrador")
                        .permissions(new HashSet<>(List.of(basePermission)))
                        .build());

        adminUser = userRepository.save(
                User.builder()
                        .email("integration@example.com")
                        .firstName("Integration")
                        .lastName("Admin")
                        .active(true)
                        .role(adminRole)
                        .registerOrigin(RegisterOriginEnum.WEB_USUARIOS.name())
                        .build());
    }

    @Test
    void getUserPermissions_returnsGroupedPermissions() {
        ResponseEntity<Map<String, List<String>>> response =
                permissionController.getUserPermissions(adminUser.getUserId());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsKey("catalogue");
        assertThat(response.getBody().get("catalogue")).containsExactly("VIEW");
    }

    @Test
    void addPermissionToUser_persistsPermissionOnRole() {
        Permission editPermission = permissionRepository.save(
                Permission.builder()
                        .moduleCode("catalogue")
                        .permissionName("EDIT")
                        .active(true)
                        .build());

        ResponseEntity<String> response = permissionController.addPermissionToUser(
                adminUser.getUserId(),
                AddPermissionRequest.builder()
                        .moduleCode(editPermission.getModuleCode())
                        .permissionName(editPermission.getPermissionName())
                        .build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Role reloaded = roleRepository.findById(adminRole.getId()).orElseThrow();
        assertThat(reloaded.getPermissions())
                .extracting(Permission::getPermissionName)
                .contains("EDIT", "VIEW");
    }

    @Test
    void removePermissionFromUser_removesAssociation() {
        Permission removable = permissionRepository.save(
                Permission.builder()
                        .moduleCode("catalogue")
                        .permissionName("DELETE")
                        .active(true)
                        .build());
        adminRole.getPermissions().add(removable);
        roleRepository.save(adminRole);

        ResponseEntity<String> response = permissionController.removePermissionFromUser(
                adminUser.getUserId(),
                RemovePermissionRequest.builder()
                        .moduleCode(removable.getModuleCode())
                        .permissionName(removable.getPermissionName())
                        .build());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Role reloaded = roleRepository.findById(adminRole.getId()).orElseThrow();
        assertThat(reloaded.getPermissions())
                .extracting(Permission::getPermissionName)
                .doesNotContain("DELETE");
    }
}
