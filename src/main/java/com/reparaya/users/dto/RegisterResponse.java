package com.reparaya.users.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
@Builder
public class RegisterResponse {
    private String message;
    private UserDto user;
    private List<Object> zones;
    private List<Object> skills;
}
