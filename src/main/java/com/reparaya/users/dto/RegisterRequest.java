package com.reparaya.users.dto;

import com.reparaya.users.entity.UserRole;
import lombok.Getter;

@Getter
public class RegisterRequest {
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String address;
    private UserRole role;
}
