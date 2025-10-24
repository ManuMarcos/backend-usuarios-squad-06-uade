package com.reparaya.users.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.List;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateUserRequest {
    @Email(message = "Formato de email inválido")
    private String email;
    private String firstName;
    private String lastName;
    private String dni;
    private String phoneNumber;
    private List<AddressInfo> address;
}