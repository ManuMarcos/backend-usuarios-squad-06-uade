package com.reparaya.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private UserInfoLoginResponse userInfo;
    private String message;
}
