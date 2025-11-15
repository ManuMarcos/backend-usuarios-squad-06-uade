package com.reparaya.users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
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
