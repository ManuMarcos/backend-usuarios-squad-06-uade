package com.reparaya.users.dto;

import lombok.Data;

@Data
public class ResetPasswordRequest {
    private String email;
    private String oldPassword;
    private String newPassword;
}
