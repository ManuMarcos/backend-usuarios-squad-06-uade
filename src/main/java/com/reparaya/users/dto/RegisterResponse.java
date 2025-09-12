package com.reparaya.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private String message;
    private String email;
    private String role;
}
