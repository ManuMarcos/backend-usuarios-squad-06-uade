package com.reparaya.users.service;

import com.reparaya.users.dto.AddPermissionRequest;
import com.reparaya.users.dto.PermissionDto;
import com.reparaya.users.dto.RemovePermissionRequest;
import com.reparaya.users.entity.Permission;
import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import com.reparaya.users.repository.PermissionRepository;
import com.reparaya.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private PermissionRepository permissionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PermissionService permissionService;

    private Role role;
    private User user;

    @BeforeEach
    void setUp() {
        role = Role.builder()
                .id(10L)
                .name("ROLE_ADMIN")
                .permissions(new HashSet<>())
                .build();

        user = User.builder()
                .userId(1L)
                .role(role)
                .active(true)
                .build();
    }

    @Test
    void getPermissionsForUser_returnsGroupedPermissions() {
        Permission catalogue = Permission.builder()
                .moduleCode("catalogue")
                .permissionName("READ")
                .build();
        Permission catalogue2 = Permission.builder()
                .moduleCode("catalogue")
                .permissionName("WRITE")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(permissionRepository.findPermissionsByRoleId(role.getId()))
                .thenReturn(List.of(catalogue, catalogue2));

        Map<String, List<String>> permissions = permissionService.getPermissionsForUser(1L);

        assertThat(permissions).containsKey("catalogue");
        assertThat(permissions.get("catalogue")).containsExactlyInAnyOrder("READ", "WRITE");
    }

    @Test
    void getPermissionsForUserInModule_filtersByModule() {
        Permission perm = Permission.builder()
                .moduleCode("catalogue")
                .permissionName("DELETE")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(permissionRepository.findPermissionsByRoleIdAndModuleCode(role.getId(), "catalogue"))
                .thenReturn(List.of(perm));

        List<String> permissions = permissionService.getPermissionsForUserInModule(1L, "catalogue");

        assertThat(permissions).containsExactly("DELETE");
    }

    @Test
    void hasPermission_returnsFalseWhenUserInactive() {
        user.setActive(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        boolean result = permissionService.hasPermission(1L, "catalogue", "READ");

        assertFalse(result);
        verify(permissionRepository, never()).findPermissionsByRoleIdAndModuleCode(anyLong(), anyString());
    }

    @Test
    void hasPermission_returnsTrueWhenPermissionFound() {
        Permission perm = Permission.builder()
                .moduleCode("catalogue")
                .permissionName("READ")
                .active(true)
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(permissionRepository.findPermissionsByRoleIdAndModuleCode(role.getId(), "catalogue"))
                .thenReturn(List.of(perm));

        boolean result = permissionService.hasPermission(1L, "catalogue", "READ");

        assertThat(result).isTrue();
    }

    @Test
    void addPermissionToUser_persistsUpdatedRole() {
        AddPermissionRequest request = AddPermissionRequest.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();
        Permission permission = Permission.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(permissionRepository.findAll()).thenReturn(List.of(permission));

        permissionService.addPermissionToUser(1L, request);

        assertThat(role.getPermissions()).contains(permission);
        verify(userRepository).save(user);
    }

    @Test
    void addPermissionToUser_throwsWhenAlreadyPresent() {
        AddPermissionRequest request = AddPermissionRequest.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();
        Permission permission = Permission.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();
        role.getPermissions().add(permission);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(permissionRepository.findAll()).thenReturn(List.of(permission));

        assertThrows(IllegalStateException.class,
                () -> permissionService.addPermissionToUser(1L, request));
    }

    @Test
    void removePermissionFromUser_removesAndPersists() {
        Permission permission = Permission.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();
        role.getPermissions().add(permission);
        RemovePermissionRequest request = RemovePermissionRequest.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(permissionRepository.findAll()).thenReturn(List.of(permission));

        permissionService.removePermissionFromUser(1L, request);

        assertThat(role.getPermissions()).doesNotContain(permission);
        verify(userRepository).save(user);
    }

    @Test
    void syncUserPermissionsWithRole_fetchesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        permissionService.syncUserPermissionsWithRole(1L);

        verify(userRepository).findById(1L);
    }
}
