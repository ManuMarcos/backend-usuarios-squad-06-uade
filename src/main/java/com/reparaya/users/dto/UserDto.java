package com.reparaya.users.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UserDto {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private List<AddressInfo> address;
    private String dni;
    private String role;
    private Boolean active;
    private String profileImageUrl;
}
