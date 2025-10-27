package com.reparaya.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenValidationRequest {
    @NotBlank
    private String token;
}
