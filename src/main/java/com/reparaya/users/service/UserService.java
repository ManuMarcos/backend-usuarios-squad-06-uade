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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.reparaya.users.entity.Role;
import com.reparaya.users.repository.RoleRepository;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Map;
import java.util.Optional;

import static com.reparaya.users.mapper.AddressMapper.mapAddressInfoListToAddressList;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final LdapUserService ldapUserService;
    private final PermissionService permissionService;
    private final JwtUtil jwtUtil;
    private final CorePublisherService corePublisherService;

    public static final String SUCCESS_PWD_RESET = "Contraseña cambiada con éxito";
    public static final String ERROR_PWD_RESET = "Ocurrió un error al intentar cambiar la contraseña. Intente nuevamente o contáctese con un administrador";
    public static final String SUCCESS_USER_UPDATE = "Usuario actualizado con éxito";
    public static final String SUCCESS_LOGIN = "Login exitoso";
    public static final String USER_NOT_ACTIVE_LOGIN = "Login fallido. El usuario no está activo aún. Reintente nuevamente.";
    public static final String SUCCESS_REGISTER = "Usuario registrado exitosamente.";

    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<UserDto> getAllUsersDto() {
        List<User> users = getAllUsers();
        if (!users.isEmpty()) {
            return users.stream().map(u -> getUserDtoById(u.getUserId())).toList();
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No registered users found.");
    }
    
    @Transactional(readOnly = true)
    public Optional<User> getUserEntityById(Long id) {
        return userRepository.findById(id);
    }

    public UserDto getUserDtoById(Long id) {
        Optional<User> userOpt = getUserEntityById(id);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            return mapUserToDto(user);
        }
        throw new RuntimeException("User " + id + " not found");
    }

    @Transactional(readOnly = true)
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User createUser(RegisterRequest request, CoreMessage.Destination destination) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.getEmail());
        }
        
        if (ldapUserService.userExistsInLdap(request.getEmail())) {
            throw new IllegalArgumentException("El email ya existe en LDAP: " + request.getEmail());
        }

        String normalized = normalizeRoleName(request.getRole());
        Role role = roleRepository.findByName(normalized)
                .orElseThrow(() -> new IllegalArgumentException("Rol no encontrado: " + normalized));

        String origin = RegisterOriginEnum.WEB_USUARIOS.name();

        if (destination != null) {
            if (destination.getTopic().equalsIgnoreCase("usuario")) {
                origin = RegisterOriginEnum.BUSQUEDA_SOLICITUDES.name();
            }
            if (destination.getTopic().equalsIgnoreCase("prestador")) {
                origin = RegisterOriginEnum.CATALOGO.name();
            }
        }

        User newUser = User.builder()
            .email(request.getEmail())
            .firstName(request.getFirstName())
            .lastName(request.getLastName())
            .phoneNumber(request.getPhoneNumber())
            .role(role)
            .dni(request.getDni())
            .active(false)
            .registerOrigin(origin)
            .build();

        if (request.getAddress() != null) {
            newUser.setAddresses(mapAddressInfoListToAddressList(request.getAddress(), newUser));
        }

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

    private UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .active(user.getActive())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .dni(user.getDni())
                .addresses(user.getAddresses() != null ? user.getAddresses().stream().map(AddressMapper::toDto).toList() : Collections.emptyList())
                .build();
    }

    private String normalizeRoleName(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Debe enviar role");
        }
        return raw.trim().toUpperCase();
    }

    public void registerUserFromEvent(RegisterRequest request, CoreMessage event) {

        User savedUser = createUser(request, event.getDestination());

        RegisterResponse response = new RegisterResponse(
                SUCCESS_REGISTER,
                mapUserToDto(savedUser),
                request.getZones(),
                request.getSkills());

        corePublisherService.sendUserCreatedToCore(response);

        activateUserAfterRegistration(savedUser.getUserId());
    }

    @Transactional
    public void deactivateUserFromEvent(final Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404), "User not found with id: " + userId));
        if (!user.getActive()) {
            log.error("Tried to deactivate an inactive user. User id: {}", userId);
            throw new IllegalStateException("Cannot deactivate an inactive user. User id: " + userId);
        }
        user.setActive(false);
        userRepository.save(user);
    }

    @Transactional
    public void activateUserAfterRegistration(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        if (!user.getActive()) {
            user.setActive(true);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        }
    }


    public RegisterResponse registerUser(RegisterRequest request) {

        User savedUser = createUser(request, null);

        RegisterResponse response = new RegisterResponse(
                SUCCESS_REGISTER,
                mapUserToDto(savedUser),
                null,
                null);

        corePublisherService.sendUserCreatedToCore(response);

        // TODO: enviar email

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

        if (!user.getActive()) {
            return new LoginResponse(
                    null,
                    null,
                    USER_NOT_ACTIVE_LOGIN
            );
        }

        // "search": ["permission_1", "permission_2", ...],
        // "catalog": ["permission_3", "permission_4", ...], ...
        Map<String, List<String>> permissionsPerModule = permissionService.getPermissionsForUser(user.getUserId());

        String token = jwtUtil.generateToken(user, permissionsPerModule);

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
        Optional<User> optUser = getUserEntityById(userId);

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

        String oldEmail = user.getEmail();

        if (request.getEmail() != null) {
            if (!oldEmail.equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("El email ya se encuentra registrado.");
            }
            user.setEmail(request.getEmail());
        }
        if (request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if (request.getLastName() != null) user.setLastName(request.getLastName());
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        if (request.getAddress() != null) {
            List<Address> newAddresses = mapAddressInfoListToAddressList(request.getAddress(), user);
            user.getAddresses().clear();
            user.getAddresses().addAll(newAddresses);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        log.info("Partially updated user: {}", updatedUser.getEmail());

        boolean ldapUpdated;

        if (request.getPassword() != null) {
            ldapUpdated = ldapUserService.updateUserInLdapWithNewPwd(oldEmail, updatedUser, request.getPassword());
        } else {
            ldapUpdated = ldapUserService.updateUserInLdap(oldEmail, updatedUser);
        }
        if (!ldapUpdated) {
            log.error("Could not update user: {} in ldap", oldEmail);
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