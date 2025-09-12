package com.reparaya.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserChangeActiveRequest {
    private boolean active;
}
