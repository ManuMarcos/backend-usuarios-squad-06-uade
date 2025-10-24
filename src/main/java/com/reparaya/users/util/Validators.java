package com.reparaya.users.util;

import com.reparaya.users.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

public class Validators {

    public static void validateRequest(RegisterRequest request) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request inválido");
        }

        List<String> errors = new ArrayList<>();

        String email = safeTrim(request.getEmail());
        String password = safeTrim(request.getPassword());
        String firstName = safeTrim(request.getFirstName());
        String lastName = safeTrim(request.getLastName());
        String dni = safeTrim(request.getDni());
        String phone = safeTrim(request.getPhoneNumber());
        String role = safeTrim(request.getRole());

        if (!hasText(email)) errors.add("El email es obligatorio.");
        else if (!isValidEmail(email)) errors.add("Formato de email inválido.");

        if (!hasText(password)) errors.add("La contraseña es obligatoria.");
        if (!hasText(firstName)) errors.add("El nombre es obligatorio.");
        if (!hasText(lastName)) errors.add("El apellido es obligatorio.");
        if (!hasText(dni)) errors.add("El DNI es obligatorio.");
        if (!hasText(phone)) errors.add("El número de celular/telefono es obligatorio.");
        if (!hasText(role)) errors.add("El rol es obligatorio.");

        if (!errors.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.join(" ", errors));
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String safeTrim(String s) {
        return s == null ? null : s.trim();
    }

    private static boolean isValidEmail(String email) {
        if (email == null) return false;
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        return email.matches(emailRegex);
    }
}
