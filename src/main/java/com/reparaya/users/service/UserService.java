package com.reparaya.users.service;

import com.reparaya.users.dto.*;
import com.reparaya.users.entity.Address;
import com.reparaya.users.entity.User;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.mapper.AddressMapper;
import com.reparaya.users.repository.UserRepository;
import com.reparaya.users.util.JwtUtil;
import com.reparaya.users.util.RegisterOriginEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.reparaya.users.entity.Role;
import com.reparaya.users.repository.RoleRepository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final LdapUserService ldapUserService;
    private final JwtUtil jwtUtil;
    private final CorePublisherService corePublisherService;

    public static final String SUCCESS_PWD_RESET = "Contraseña cambiada con éxito";
    public static final String ERROR_PWD_RESET = "Ocurrió un error al intentar cambiar la contraseña. Intente nuevamente o contáctese con un administrador";
    public static final String SUCCESS_USER_UPDATE = "Usuario actualizado con éxito";
    public static final String SUCCESS_LOGIN = "Login exitoso";
    public static final String SUCCESS_REGISTER = "Usuario registrado exitosamente.";

    private final RoleRepository roleRepository;

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

        String normalized = normalizeRoleName(request.getRole());
        Role role = roleRepository.findByName(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + normalized));

        User newUser = User.builder()
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .role(role)
            .dni(request.getDni())
            .active(true)
            .registerOrigin(RegisterOriginEnum.WEB_USUARIOS.name())
            .build();

        newUser.setAddresses(mapAddress(request.getAddress(), newUser));

        User savedUser = userRepository.save(newUser);
        log.info("Usuario guardado en PostgreSQL: {}", savedUser.getEmail());

        try {
            User ldapUser = User.builder()
                .email(savedUser.getEmail())
                .firstName(savedUser.getFirstName())
                .lastName(savedUser.getLastName())
                .phoneNumber(savedUser.getPhoneNumber())
                .role(savedUser.getRole())
                .dni(savedUser.getDni())
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

    private List<Address> mapAddress(List<AddressInfo> addressList, User user) {
        return addressList.stream().map(addr -> Address.builder()
                .state(addr.getState())
                .city(addr.getCity())
                .street(addr.getStreet())
                .number(addr.getNumber())
                .floor(addr.getFloor() != null ? addr.getFloor() : null)
                .apartment(addr.getApartment() != null ? addr.getApartment() : null)
                .user(user).build()).toList();
    }

    private String normalizeRoleName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Debe enviar role");
        }
        return raw.trim().toUpperCase();
    }


    public RegisterResponse registerUser(RegisterRequest request) {
        User savedUser = createUser(request);

        RegisterResponse response = new RegisterResponse(
                SUCCESS_REGISTER,
                savedUser.getEmail(),
                savedUser.getRole().getName());

        corePublisherService.sendUserCreatedToCore(response);
        
        return response;
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

        String token = jwtUtil.generateToken(user.getEmail(), user.getRole().getName());
        
        return new LoginResponse(
            token,
            UserInfoLoginResponse.builder()
                    .id(user.getUserId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .phoneNumber(user.getPhoneNumber())
                    .address(AddressMapper.toDtoList(user.getAddresses()))
                    .isActive(user.getActive())
                    .dni(user.getDni())
                    .role(user.getRole().getName())
                    .build(),
                SUCCESS_LOGIN
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

    @Transactional
    public String updateUserPartially(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario con id " + userId + " no encontrado"));

        String email = user.getEmail();

        if (request.getEmail() != null) user.setEmail(request.getEmail());
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        if (request.getDni() != null) user.setDni(request.getDni());

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        log.info("Usuario parcialmente actualizado: {}", updatedUser.getEmail());

        boolean ldapUpdated = ldapUserService.updateUserInLdap(email, updatedUser);
        if (!ldapUpdated) {
            log.error("No se pudo actualizar el usuario en LDAP: {}", email);
            throw new RuntimeException("Error al actualizar usuario en LDAP");
        }

        return SUCCESS_USER_UPDATE;
    }

    public void changeUserIsActive(Long userId, UserChangeActiveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario con id " + userId + " no encontrado"));
        user.setActive(request.isActive());
        user.setUpdatedAt(LocalDateTime.now());
    }
}