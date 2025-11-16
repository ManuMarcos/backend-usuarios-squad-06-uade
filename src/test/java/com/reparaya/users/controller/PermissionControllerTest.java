package com.reparaya.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.dto.AddPermissionRequest;
import com.reparaya.users.dto.PermissionDto;
import com.reparaya.users.dto.RemovePermissionRequest;
import com.reparaya.users.service.PermissionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController permissionController;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(permissionController).build();
    }

    @Test
    void getUserPermissions_returnsData() throws Exception {
        when(permissionService.getPermissionsForUser(1L))
                .thenReturn(Map.of("catalogue", List.of("READ")));

        mockMvc.perform(get("/api/permissions/user/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.catalogue[0]").value("READ"));
    }

    @Test
    void getUserPermissions_handlesError() throws Exception {
        when(permissionService.getPermissionsForUser(1L))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/permissions/user/{userId}", 1L))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getUserPermissionsInModule_returnsList() throws Exception {
        when(permissionService.getPermissionsForUserInModule(5L, "module_catalog"))
                .thenReturn(List.of("READ", "WRITE"));

        mockMvc.perform(get("/api/permissions/user/{userId}/module/{moduleCode}", 5L, "module_catalog"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("READ"))
                .andExpect(jsonPath("$[1]").value("WRITE"));
    }

    @Test
    void getUserPermissionsInModule_handlesException() throws Exception {
        when(permissionService.getPermissionsForUserInModule(5L, "module_catalog"))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/permissions/user/{userId}/module/{moduleCode}", 5L, "module_catalog"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void hasPermission_returnsBoolean() throws Exception {
        when(permissionService.hasPermission(8L, "module_catalog", "READ"))
                .thenReturn(true);

        mockMvc.perform(get("/api/permissions/user/{userId}/module/{moduleCode}/permission/{permission}", 8L, "module_catalog", "READ"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    void hasPermission_handlesException() throws Exception {
        when(permissionService.hasPermission(8L, "module_catalog", "READ"))
                .thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/permissions/user/{userId}/module/{moduleCode}/permission/{permission}", 8L, "module_catalog", "READ"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getAllPermissions_returnsList() throws Exception {
        when(permissionService.getAllPermissionsDto())
                .thenReturn(List.of(PermissionDto.builder().permissionName("VIEW").moduleCode("catalogue").build()));

        mockMvc.perform(get("/api/permissions/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].permissionName").value("VIEW"));
    }

    @Test
    void getAllPermissions_handlesException() throws Exception {
        when(permissionService.getAllPermissionsDto())
                .thenThrow(new RuntimeException("fail"));

        mockMvc.perform(get("/api/permissions/all"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    void getPermissionsByModule_returnsList() throws Exception {
        when(permissionService.getPermissionsByModuleDto("catalogue"))
                .thenReturn(List.of(PermissionDto.builder()
                        .moduleCode("catalogue")
                        .permissionName("VIEW")
                        .build()));

        mockMvc.perform(get("/api/permissions/module/{moduleCode}", "catalogue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].permissionName").value("VIEW"));
    }

    @Test
    void addPermissionToUser_returnsConflictWhenAlreadyExists() throws Exception {
        doThrow(new IllegalStateException("exists"))
                .when(permissionService).addPermissionToUser(any(), any(AddPermissionRequest.class));

        AddPermissionRequest request = AddPermissionRequest.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();

        mockMvc.perform(post("/api/permissions/user/{userId}/add", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void removePermissionFromUser_returnsNotFoundWhenMissing() throws Exception {
        doThrow(new IllegalArgumentException("missing"))
                .when(permissionService).removePermissionFromUser(any(), any(RemovePermissionRequest.class));

        RemovePermissionRequest request = RemovePermissionRequest.builder()
                .moduleCode("catalogue")
                .permissionName("MANAGE")
                .build();

        mockMvc.perform(delete("/api/permissions/user/{userId}/remove", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void syncPermissions_handlesSuccess() throws Exception {
        mockMvc.perform(post("/api/permissions/user/{userId}/sync", 5L))
                .andExpect(status().isOk());
    }
}
