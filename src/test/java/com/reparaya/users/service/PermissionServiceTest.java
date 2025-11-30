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
    void getPermissionsForUser_throwsWhenUserMissing() {
        when(userRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> permissionService.getPermissionsForUser(9L));
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
    void getPermissionsForUserInModule_throwsWhenUserMissing() {
        when(userRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
                () -> permissionService.getPermissionsForUserInModule(3L, "catalogue"));
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
    void hasPermission_returnsFalseWhenRepositoryThrows() {
        when(userRepository.findById(1L)).thenThrow(new RuntimeException("db down"));

        boolean result = permissionService.hasPermission(1L, "catalogue", "READ");

        assertFalse(result);
    }

    @Test
    void getAllPermissionsDto_mapsFields() {
        Permission permission = Permission.builder()
                .id(5L)
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .description("desc")
                .active(true)
                .build();

        when(permissionRepository.findAll()).thenReturn(List.of(permission));

        List<PermissionDto> dtos = permissionService.getAllPermissionsDto();

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getId()).isEqualTo(5L);
        assertThat(dtos.get(0).getPermissionName()).isEqualTo("MANAGE");
        assertThat(dtos.get(0).isActive()).isTrue();
    }

    @Test
    void getPermissionsByModuleDto_returnsMappedList() {
        Permission permission = Permission.builder()
                .id(6L)
                .moduleCode("catalogue")
                .permissionName("READ")
                .description("desc")
                .active(true)
                .build();

        when(permissionRepository.findPermissionsByModuleCode("catalogue")).thenReturn(List.of(permission));

        List<PermissionDto> dtos = permissionService.getPermissionsByModuleDto("catalogue");

        assertThat(dtos).hasSize(1);
        assertThat(dtos.get(0).getModuleCode()).isEqualTo("catalogue");
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
    void addPermissionToUser_throwsWhenPermissionMissing() {
        AddPermissionRequest request = AddPermissionRequest.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(permissionRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> permissionService.addPermissionToUser(1L, request));
    }

    @Test
    void addPermissionToUser_throwsWhenUserNotFound() {
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class,
                () -> permissionService.addPermissionToUser(2L, AddPermissionRequest.builder().build()));
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
    void removePermissionFromUser_throwsWhenPermissionMissing() {
        RemovePermissionRequest request = RemovePermissionRequest.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(permissionRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> permissionService.removePermissionFromUser(1L, request));
    }

    @Test
    void syncUserPermissionsWithRole_throwsWhenUserMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> permissionService.syncUserPermissionsWithRole(1L));
    }

    @Test
    void syncUserPermissionsWithRole_fetchesUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        permissionService.syncUserPermissionsWithRole(1L);

        verify(userRepository).findById(1L);
    }
}
