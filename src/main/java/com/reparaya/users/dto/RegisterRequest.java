package com.reparaya.users.dto;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email invalido")
    private String email;


    @NotBlank(message = "La contrasena es obligatoria")
    @Size(min = 8, message = "La contrasena debe tener al menos 8 caracteres")
    private String password;


    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;


    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;


    @NotBlank(message = "El DNI es obligatorio")
    private String dni;


    @NotBlank(message = "El numero de celular/telefono es obligatorio")
    private String phoneNumber;

    private AddressInfo primaryAddressInfo;
    private AddressInfo secondaryAddressInfo;

    @NotBlank(message = "El rol es obligatorio")
    private String role;

    private List<AddressInfo> address;

    private List<Object> zones;
    private List<Object> skills;
}
