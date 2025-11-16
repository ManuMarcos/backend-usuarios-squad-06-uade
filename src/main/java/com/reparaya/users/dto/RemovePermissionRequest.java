package com.reparaya.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RemovePermissionRequest {
    @NotBlank(message = "El código del módulo es requerido")
    private String moduleCode;
    
    @NotBlank(message = "El nombre del permiso es requerido")
    private String permissionName;
}
