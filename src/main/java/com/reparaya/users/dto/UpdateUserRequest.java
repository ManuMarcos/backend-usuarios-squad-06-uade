package com.reparaya.users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserRequest {
    @Email(message = "Formato de email inválido")
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private List<AddressInfo> address;
    private List<Object> zones;
    private List<Object> skills;
}