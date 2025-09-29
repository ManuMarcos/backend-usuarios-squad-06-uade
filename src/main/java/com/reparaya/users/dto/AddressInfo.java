package com.reparaya.users.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddressInfo {

    @NotBlank(message = "La provincia/estado es obligatorio")
    private String state;

    @NotBlank(message = "La ciudad es obligatoria")
    private String city;

    private String locality;

    @NotBlank(message = "La calle es obligatoria")
    private String street;

    @NotBlank(message = "El número es obligatorio")
    private String number;

    private String floor;

    private String apartment;

    private String postalCode;
}