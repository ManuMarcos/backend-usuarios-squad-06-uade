package com.reparaya.users.dto;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    private String password;

    @NotBlank(message = "El nombre es obligatorio")
    private String firstName;

    @NotBlank(message = "El apellido es obligatorio")
    private String lastName;

    @NotBlank(message = "El DNI es obligatorio")
    private String dni;

    @NotBlank(message = "El numero de celular/telefono es obligatorio")
    private String phoneNumber;

    @NotBlank(message = "El rol es obligatorio")
    private String role;

    private List<AddressInfo> address;

    private List<Object> zones;
    private List<Object> skills;
}
