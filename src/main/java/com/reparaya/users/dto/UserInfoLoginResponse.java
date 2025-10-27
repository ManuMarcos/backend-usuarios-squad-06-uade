package com.reparaya.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class UserInfoLoginResponse {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private List<AddressInfo> address;
    private String role;
    private boolean isActive;
    private String dni;
    private String profileImageUrl;
}