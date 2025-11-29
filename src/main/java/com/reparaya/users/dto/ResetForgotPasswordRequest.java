package com.reparaya.users.dto;

import lombok.Data;

@Data
public class ResetForgotPasswordRequest {

    private String token;
    private String newPassword;

}
