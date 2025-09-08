package com.reparaya.users.service;

import com.reparaya.users.dto.*;
import com.reparaya.users.entity.User;
import com.reparaya.users.repository.UserRepository;
import com.reparaya.users.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    public static final String SUCCESS_PWD_RESET = "Contraseña cambiada con éxito";
    public static final String ERROR_PWD_RESET = "Ocurrió un error al intentar cambiar la contraseña. Intente nuevamente o contáctese con un administrador";
    private final UserRepository userRepository;
    private final LdapUserService ldapUserService;
    private final JwtUtil jwtUtil;
    
    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }


    public User createUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.getEmail());
        }
        
        if (ldapUserService.userExistsInLdap(request.getEmail())) {
            throw new IllegalArgumentException("El email ya existe en LDAP: " + request.getEmail());
        }

        User newUser = User.builder()
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .address(request.getAddress())
            .role(request.getRole())
            .active(true)
            .build();

        User savedUser = userRepository.save(newUser);
        log.info("Usuario guardado en PostgreSQL: {}", savedUser.getEmail());

        try {
            User ldapUser = User.builder()
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .phoneNumber(savedUser.getPhoneNumber())
                .address(savedUser.getAddress())
                .role(savedUser.getRole())
                .active(savedUser.getActive())
                .build();
                
            ldapUserService.createUserInLdap(ldapUser, request.getPassword());

        } catch (Exception e) {
            userRepository.delete(savedUser);
            log.error("Error al crear usuario en LDAP, eliminando de PostgreSQL: {}", e.getMessage());
            throw new RuntimeException("Error al crear usuario en LDAP: " + e.getMessage());
        }

        return savedUser;
    }

    public RegisterResponse registerUser(RegisterRequest request) {
        User savedUser = createUser(request);
        return new RegisterResponse(
            "Usuario registrado exitosamente.",
            savedUser.getEmail(),
            savedUser.getRole().toString()
        );
    }

    public LoginResponse authenticateUser(LoginRequest request) {
        boolean ldapAuthSuccess = ldapUserService.authenticateUser(request.getEmail(), request.getPassword());
        
        if (!ldapAuthSuccess) {
            log.warn("Autenticación LDAP fallida para usuario: {}", request.getEmail());
            return new LoginResponse(null, null, "Credenciales inválidas");
        }

        Optional<User> optUser = getUserByEmail(request.getEmail());

        if (optUser.isEmpty()) {
            log.error("No se pudo obtener información del usuario: {}", request.getEmail());
            return new LoginResponse(null, null, "Error obteniendo información del usuario");
        }

        User user = optUser.get();

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().toString());
        
        return new LoginResponse(
            token,
            UserInfoLoginResponse.builder()
                    .id(user.getUserId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .address(user.getAddress())
                    .isActive(user.getActive())
                    .build(),
            "Login exitoso"
        );
    }

    public boolean validateTokenAndUser(String token, String email) {
        return jwtUtil.validateToken(token, email);
    }

    public String extractEmailFromToken(String token) {
        return jwtUtil.extractEmail(token);
    }

    public String extractRoleFromToken(String token) {
        return jwtUtil.extractRole(token);
    }

    @Transactional
    public String resetPassword(Long userId, String newPassword) {
        Optional<User> optUser = getUserById(userId);

        if (optUser.isEmpty()) {
            throw new RuntimeException("El usuario con id: " + userId + " no existe.");
        }

        if (ldapUserService.resetUserPassword(optUser.get(), newPassword)) {
            optUser.get().setUpdatedAt(LocalDateTime.now());
            userRepository.save(optUser.get());
            return SUCCESS_PWD_RESET;
        } else {
            return ERROR_PWD_RESET;
        }
    }
}
