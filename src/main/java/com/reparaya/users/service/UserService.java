package com.reparaya.users.service;

import com.reparaya.users.dto.*;
import com.reparaya.users.entity.Address;
import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.mapper.AddressMapper;
import com.reparaya.users.repository.RoleRepository;
import com.reparaya.users.repository.UserRepository;
import com.reparaya.users.util.JwtUtil;
import com.reparaya.users.util.RegisterOriginEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.*;

import static com.reparaya.users.mapper.AddressMapper.mapAddressInfoListToAddressList;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    public static final String ADMIN_ROLE = "ADMIN";
    private final UserRepository userRepository;
    private final LdapUserService ldapUserService;
    private final PermissionService permissionService;
    private final JwtUtil jwtUtil;
    private final CorePublisherService corePublisherService;
    private final S3StorageService s3StorageService;

    private static final String FIRSTNAME_LASTNAME_REGEX = "^[\\p{L}]+(?: [\\p{L}]+)*$";
    private static final String DNI_REGEX = "^\\d{7,10}$";
    private static final String PHONE_REGEX = "^[0-9()+\\- ]*$";
    private static final String PASSWORD_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$";

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
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new IllegalArgumentException("El email ya está registrado: " + request.getEmail());
        }

        if (ldapUserService.userExistsInLdap(request.getEmail().toLowerCase().trim())) {
            throw new IllegalArgumentException("El email ya existe en LDAP: " + request.getEmail());
        }

        String normalized = normalizeRoleName(request.getRole());
        Role role = roleRepository.findByName(normalized)
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404),"Rol no encontrado: " + normalized));

        String origin = RegisterOriginEnum.WEB_USUARIOS.name();

        if (destination != null) {
            if (destination.getTopic().equalsIgnoreCase("usuario")) {
                origin = RegisterOriginEnum.BUSQUEDA_SOLICITUDES.name();
            }
            if (destination.getTopic().equalsIgnoreCase("prestador")) {
                origin = RegisterOriginEnum.CATALOGO.name();
            }
        }

        if (!request.getDni().matches(DNI_REGEX)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400),"El dni debe tener de 7 a 10 caracteres y no debe contener letras");
        }

        String email = request.getEmail().trim().toLowerCase();
        String firstName = StringUtils.capitalize(request.getFirstName().trim().toLowerCase());
        String lastName = StringUtils.capitalize(request.getLastName().trim().toLowerCase());

        if (!firstName.matches(FIRSTNAME_LASTNAME_REGEX) || !lastName.matches(FIRSTNAME_LASTNAME_REGEX)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400),"El nombre y apellido no deben contener numeros, simbolos o multiples espacios.");
        }

        String phoneNumber = request.getPhoneNumber().trim();

        if (!phoneNumber.matches(PHONE_REGEX)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400),"El numero de telefono solo acepta numeros, +, - o ( ).");
        }

        if (!request.getPassword().matches(PASSWORD_REGEX)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400),"La contraseña debe tener mas de 8 digitos, al menos una mayuscula, al menos una minuscula y al menos un simbolo.");

        }

        User newUser = User.builder()
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .phoneNumber(phoneNumber)
                .role(role)
                .dni(request.getDni().trim())
                .active(true)
                .registerOrigin(origin)
                .build();

        if (request.getAddress() != null) {
            newUser.setAddress(mapAddressInfoListToAddressList(request.getAddress(), newUser));
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

    UserDto mapUserToDto(User user) {
        return UserDto.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .active(user.getActive())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .dni(user.getDni())
                .profileImageUrl(user.getProfileImageUrl())
                .address(user.getAddress() != null ? user.getAddress().stream().map(AddressMapper::toDto).toList() : Collections.emptyList())
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
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404),"User not found with id: " + userId));
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

        return response;
    }


    public LoginResponse authenticateUser(LoginRequest request) {
        boolean ldapAuthSuccess = ldapUserService.authenticateUser(request.getEmail(), request.getPassword());

        if (!ldapAuthSuccess) {
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
                        .address(AddressMapper.toDtoList(user.getAddress()))
                        .profileImageUrl(user.getProfileImageUrl())
                        .isActive(user.getActive())
                        .dni(user.getDni())
                        .role(user.getRole().getName())
                        .build(),
                SUCCESS_LOGIN
        );
    }


    @Transactional
    public String resetPassword(Long userId, String newPassword) {
        Optional<User> optUser = getUserEntityById(userId);

        if (optUser.isEmpty()) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(404),"El usuario con id: " + userId + " no existe.");
        }

        if (!newPassword.matches(PASSWORD_REGEX)) {
            throw new ResponseStatusException(HttpStatusCode.valueOf(400),"La contraseña debe tener mas de 8 digitos, al menos una mayuscula, al menos una minuscula y al menos un simbolo.");
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
    public UpdateUserResponse updateUserPartiallyFromEvent(UpdateUserRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404),"Usuario con id " + request.getUserId() + " no encontrado"));

        String oldEmail = user.getEmail();

        if (request.getEmail() != null) {
            if (!oldEmail.equals(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
                throw new RuntimeException("El email ya se encuentra registrado.");
            }
            user.setEmail(request.getEmail().toLowerCase().trim());
        }
        if (request.getFirstName() != null) user.setFirstName(StringUtils.capitalize(request.getFirstName().toLowerCase().trim()));
        if (request.getLastName() != null) user.setLastName(StringUtils.capitalize(request.getLastName().toLowerCase().trim()));
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber().trim());

        if (request.getAddress() != null) {
            List<Address> newAddresses = mapAddressInfoListToAddressList(request.getAddress(), user);
            user.getAddress().clear();
            user.getAddress().addAll(newAddresses);
        }

        if (request.getProfileImageUrl() != null) {
            try {
                // Descargar imagen desde la URL y subirla a S3
                String s3ImageUrl = s3StorageService.downloadAndUploadImageFromUrl(
                    request.getProfileImageUrl(), 
                    user.getUserId()
                );
                user.setProfileImageUrl(s3ImageUrl);
                log.info("Profile image download and uploaded to S3. User: {}", user.getEmail());
            } catch (Exception e) {
                log.error("Error while processing image for user  {}: {}",
                    user.getEmail(), e.getMessage(), e);
                throw new RuntimeException("Error al procesar imagen de perfil: " + e.getMessage(), e);
            }
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

        return UpdateUserResponse.builder()
                .user(mapUserToDto(updatedUser))
                .zones(request.getZones())
                .skills(request.getSkills())
                .build();
    }

    @Transactional
    public String updateUserPartially(Long userId, UpdateUserRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404),"Usuario con id " + userId + " no encontrado"));

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("Usuario no autenticado");
        }

        String authenticatedEmail = authentication.getPrincipal().toString();

        String oldEmail = user.getEmail();

        if (!authenticatedEmail.equalsIgnoreCase(oldEmail) && !ADMIN_ROLE.equalsIgnoreCase(user.getRole().getName())) {
            throw new IllegalArgumentException("Solo un usuario administrador o el usuario correspondiente puede editar datos.");
        }

        if (request.getEmail() != null) {
            if (!oldEmail.equals(request.getEmail().toLowerCase().trim()) && userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
                throw new RuntimeException("El email ya se encuentra registrado.");
            }
            user.setEmail(request.getEmail().toLowerCase().trim());
        }
        if (request.getFirstName() != null) user.setFirstName(StringUtils.capitalize(request.getFirstName().toLowerCase().trim()));
        if (request.getLastName() != null) user.setLastName(StringUtils.capitalize(request.getLastName().toLowerCase().trim()));
        if (request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber().trim());

        if (request.getAddress() != null) {
            List<Address> newAddresses = mapAddressInfoListToAddressList(request.getAddress(), user);
            user.getAddress().clear();
            user.getAddress().addAll(newAddresses);
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

        var response = UpdateUserResponse.builder()
                .zones(new ArrayList<>())
                .skills(new ArrayList<>())
                .user(mapUserToDto(updatedUser))
                .build();

        corePublisherService.sendUserUpdatedToCore(response);

        return SUCCESS_USER_UPDATE;
    }


    public void changeUserIsActive(Long userId, UserChangeActiveRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404),"Usuario con id " + userId + " no encontrado"));
        user.setActive(request.isActive());
        user.setUpdatedAt(LocalDateTime.now());
    }
    
    @Transactional
    public User updateUserProfileImage(String email, String profileImageUrl) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatusCode.valueOf(404),"Usuario con email " + email + " no encontrado"));
        user.setProfileImageUrl(profileImageUrl);
        user.setUpdatedAt(LocalDateTime.now());
        var userUpdated = userRepository.save(user);
        log.info("Imagen de perfil actualizada para usuario: {}", email);
        return userUpdated;
    }
}