package com.reparaya.users.service;

import com.reparaya.users.dto.AddressInfo;
import com.reparaya.users.dto.CoreMessage;
import com.reparaya.users.dto.LoginRequest;
import com.reparaya.users.dto.LoginResponse;
import com.reparaya.users.dto.RegisterRequest;
import com.reparaya.users.dto.RegisterResponse;
import com.reparaya.users.dto.ResetPasswordRequest;
import com.reparaya.users.dto.UpdateUserRequest;
import com.reparaya.users.dto.UserChangeActiveRequest;
import com.reparaya.users.dto.UserDto;
import com.reparaya.users.external.service.CorePublisherService;
import com.reparaya.users.entity.Address;
import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import com.reparaya.users.repository.RoleRepository;
import com.reparaya.users.repository.UserRepository;
import com.reparaya.users.util.RegisterOriginEnum;
import com.reparaya.users.util.JwtUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LdapUserService ldapUserService;
    @Mock private JwtUtil jwtUtil;
    @Mock private PermissionService permissionService;
    @Mock private RoleRepository roleRepository;
    @Mock private CorePublisherService corePublisherService;

    @InjectMocks private UserService userService;

    private AutoCloseable mocks;
    private final ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() throws Exception {
        mocks.close();
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAllUsersDto_whenRepositoryEmpty_throwsNotFound() {
        when(userRepository.findAll()).thenReturn(Collections.emptyList());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.getAllUsersDto());

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        verify(userRepository).findAll();
    }

    @Test
    void getAllUsersDto_returnsDtosUsingMapper() {
        User user = new User();
        user.setUserId(7L);
        when(userRepository.findAll()).thenReturn(List.of(user));

        UserService spyService = spy(userService);
        UserDto dto = UserDto.builder().userId(7L).email("user@demo.com").build();
        doReturn(dto).when(spyService).getUserDtoById(7L);

        List<UserDto> result = spyService.getAllUsersDto();

        assertEquals(1, result.size());
        assertEquals("user@demo.com", result.get(0).getEmail());
        verify(userRepository).findAll();
        verify(spyService).getUserDtoById(7L);
    }

    @Test
    void changeUserIsActive_updatesFlagAndTimestamp() {
        Long userId = 1L;
        User existing = new User();
        existing.setUserId(userId);
        existing.setActive(false);
        existing.setUpdatedAt(LocalDateTime.now().minusDays(1));

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        UserChangeActiveRequest req = new UserChangeActiveRequest();
        req.setActive(true);

        userService.changeUserIsActive(userId, req);

        assertTrue(existing.getActive());
        assertNotNull(existing.getUpdatedAt());
        assertTrue(existing.getUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(1)));

        verify(userRepository).findById(userId);
        verify(userRepository).save(existing);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void changeUserIsActive_throwsWhenUserNotFound() {
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                userService.changeUserIsActive(userId, new UserChangeActiveRequest()));

        assertTrue(ex.getMessage().toLowerCase().contains("no encontrado"));
        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void changeUserIsActive_persistsUpdatedEntity() {
        Long userId = 12L;
        User existing = new User();
        existing.setUserId(userId);
        existing.setActive(true);
        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));

        UserChangeActiveRequest request = new UserChangeActiveRequest();
        request.setActive(false);

        userService.changeUserIsActive(userId, request);

        verify(userRepository).findById(userId);
        verify(userRepository).save(existing);
        assertFalse(existing.getActive());
    }

    @Test
    void deactivateUserFromEvent_throwsWhenAlreadyInactive() {
        Long userId = 20L;
        User user = new User();
        user.setUserId(userId);
        user.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> userService.deactivateUserFromEvent(userId));
        assertTrue(ex.getMessage().toLowerCase().contains("inactive"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void activateUserAfterRegistration_setsActiveWhenInactive() {
        Long userId = 21L;
        User user = new User();
        user.setUserId(userId);
        user.setActive(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        userService.activateUserAfterRegistration(userId);

        assertTrue(user.getActive());
        assertNotNull(user.getUpdatedAt());
        verify(userRepository).save(user);
    }

    @Test
    void updateUserProfileImage_updatesUrlAndTimestamp() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setUpdatedAt(LocalDateTime.now().minusDays(1));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        User updated = userService.updateUserProfileImage("user@example.com", "https://img");

        assertEquals("https://img", updated.getProfileImageUrl());
        assertTrue(updated.getUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(1)));
        verify(userRepository).save(user);
    }

    @Test
    void createUser_persistsUserAndCreatesLdap() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getEmail()).thenReturn("new@example.com");
        when(request.getRole()).thenReturn(" admin ");
        when(request.getFirstName()).thenReturn("John");
        when(request.getLastName()).thenReturn("Doe");
        when(request.getPhoneNumber()).thenReturn("+54 11 1234 5678");
        when(request.getDni()).thenReturn("12345678");
        when(request.getPassword()).thenReturn("Secret123!");
        AddressInfo primary = AddressInfo.builder()
                .state("Buenos Aires")
                .city("Avellaneda")
                .street("Belgrano")
                .number("123")
                .build();
        when(request.getAddress()).thenReturn(Collections.singletonList(primary));

        when(userRepository.existsByEmailOrDni("new@example.com", "12345678")).thenReturn(false);
        when(ldapUserService.userExistsInLdap("new@example.com")).thenReturn(false);
        Role role = Role.builder().id(1L).name("ROLE_ADMIN").description("Admin").active(true).build();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(42L);
            return user;
        });

        User created = userService.createUser(request, null);

        assertNotNull(created);
        assertEquals(42L, created.getUserId());
        assertEquals("new@example.com", created.getEmail());
        assertEquals(1, created.getAddress().size());
        Address storedAddress = created.getAddress().get(0);
        assertEquals("Avellaneda", storedAddress.getCity());
        assertEquals(created, storedAddress.getUser());

        verify(userRepository).existsByEmailOrDni("new@example.com", "12345678");
        verify(ldapUserService).userExistsInLdap("new@example.com");
        verify(roleRepository).findByName("ADMIN");
        verify(userRepository).save(userCaptor.capture());

        User entitySaved = userCaptor.getValue();
        assertEquals("new@example.com", entitySaved.getEmail());
        assertEquals("ROLE_ADMIN", entitySaved.getRole().getName());
        assertTrue(entitySaved.getActive());
        assertEquals(RegisterOriginEnum.WEB_USUARIOS.name(), entitySaved.getRegisterOrigin());

        verify(ldapUserService).createUserInLdap(
                argThat(u -> "new@example.com".equals(u.getEmail()) && "John".equals(u.getFirstName())),
                eq("Secret123!")
        );
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void createUser_whenEmailExists_throwsExceptionAndSkipsLdap() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getEmail()).thenReturn("duplicate@example.com");
        when(request.getDni()).thenReturn("12345678");

        when(userRepository.existsByEmailOrDni("duplicate@example.com", "12345678")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request, null));
        assertTrue(ex.getMessage().toLowerCase().contains("email o el dni"));

        verify(userRepository).existsByEmailOrDni("duplicate@example.com", "12345678");
        verifyNoInteractions(ldapUserService);
        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    void createUser_whenRoleDoesNotExist_throwsException() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getEmail()).thenReturn("role-missing@example.com");
        when(request.getRole()).thenReturn("manager");
        when(request.getDni()).thenReturn("12345678");
        when(request.getFirstName()).thenReturn("First");
        when(request.getLastName()).thenReturn("Last");
        when(request.getPhoneNumber()).thenReturn("1234567");
        when(request.getPassword()).thenReturn("Secret123!");

        when(userRepository.existsByEmailOrDni("role-missing@example.com", "12345678")).thenReturn(false);
        when(ldapUserService.userExistsInLdap("role-missing@example.com")).thenReturn(false);
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.createUser(request, null));
        assertTrue(ex.getReason().contains("Rol no encontrado"));

        verify(userRepository).existsByEmailOrDni("role-missing@example.com", "12345678");
        verify(ldapUserService).userExistsInLdap("role-missing@example.com");
        verify(roleRepository).findByName("MANAGER");
        verify(userRepository, never()).save(any(User.class));
        verify(ldapUserService, never()).createUserInLdap(any(User.class), any());
    }

    @Test
    void createUser_whenRoleBlank_throwsException() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getEmail()).thenReturn("blank@example.com");
        when(request.getRole()).thenReturn("   ");
        when(request.getDni()).thenReturn("12345678");
        when(request.getFirstName()).thenReturn("First");
        when(request.getLastName()).thenReturn("Last");
        when(request.getPhoneNumber()).thenReturn("1234567");
        when(request.getPassword()).thenReturn("Secret123!");
        when(userRepository.existsByEmailOrDni("blank@example.com", "12345678")).thenReturn(false);
        when(ldapUserService.userExistsInLdap("blank@example.com")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request, null));

        assertTrue(ex.getMessage().contains("Debe enviar role"));
        verify(userRepository).existsByEmailOrDni("blank@example.com", "12345678");
        verify(ldapUserService).userExistsInLdap("blank@example.com");
        verifyNoInteractions(roleRepository);
    }

    @Test
    void createUser_fromPrestadorEventSetsOriginAndAddress() {
        RegisterRequest request = RegisterRequest.builder()
                .email("catalog@example.com")
                .password("Pass12345!")
                .firstName("Cat")
                .lastName("User")
                .phoneNumber("555")
                .dni("12345678")
                .role("prestador")
                .address(List.of(AddressInfo.builder()
                        .state("Buenos Aires")
                        .city("CABA")
                        .street("Libertad")
                        .number("100")
                        .build()))
                .build();
        CoreMessage.Destination destination = new CoreMessage.Destination();
        destination.setTopic("prestador");
        destination.setEventName("user_created");

        when(userRepository.existsByEmailOrDni("catalog@example.com", "12345678")).thenReturn(false);
        when(ldapUserService.userExistsInLdap("catalog@example.com")).thenReturn(false);
        Role role = Role.builder().id(9L).name("PRESTADOR").build();
        when(roleRepository.findByName("PRESTADOR")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setUserId(55L);
            return saved;
        });

        User created = userService.createUser(request, destination);

        assertEquals(RegisterOriginEnum.CATALOGO.name(), created.getRegisterOrigin());
        assertEquals(1, created.getAddress().size());
        Address mappedAddress = created.getAddress().get(0);
        assertEquals("Caba", mappedAddress.getCity());
        assertEquals("Buenos aires", mappedAddress.getState());
        verify(userRepository).save(created);
        verify(ldapUserService).createUserInLdap(any(User.class), eq("Pass12345!"));
    }

    @Test
    void createUser_whenLdapCreationFails_rollsBackAndThrows() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getEmail()).thenReturn("rollback@example.com");
        when(request.getRole()).thenReturn("user");
        when(request.getFirstName()).thenReturn("Jane");
        when(request.getLastName()).thenReturn("Doe");
        when(request.getPhoneNumber()).thenReturn("111");
        when(request.getDni()).thenReturn("99999999");
        when(request.getPassword()).thenReturn("Secret123!");
        AddressInfo primary = AddressInfo.builder()
                .state("BA")
                .city("Lanus")
                .street("Ituzaingo")
                .number("456")
                .build();
        when(request.getAddress()).thenReturn(Collections.singletonList(primary));

        when(userRepository.existsByEmailOrDni("rollback@example.com", "99999999")).thenReturn(false);
        when(ldapUserService.userExistsInLdap("rollback@example.com")).thenReturn(false);
        Role role = Role.builder().id(5L).name("ROLE_USER").build();
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(7L);
            return u;
        });
        doThrow(new RuntimeException("LDAP error"))
                .when(ldapUserService).createUserInLdap(any(User.class), eq("Secret123!"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.createUser(request, null));
        assertTrue(ex.getMessage().contains("LDAP"));

        verify(userRepository).save(userCaptor.capture());
        User persisted = userCaptor.getValue();
        assertEquals("rollback@example.com", persisted.getEmail());

        verify(ldapUserService).createUserInLdap(any(User.class), eq("Secret123!"));
        verify(userRepository).delete(persisted);
    }

    @Test
    void registerUser_returnsResponseFromCreatedUser() {
        RegisterRequest request = mock(RegisterRequest.class);
        Role role = Role.builder().name("ROLE_ADMIN").build();
        User created = User.builder()
                .userId(10L)
                .email("register@example.com")
                .role(role)
                .build();

        UserService spyService = org.mockito.Mockito.spy(userService);
        doReturn(created).when(spyService).createUser(eq(request), isNull());

        RegisterResponse response = spyService.registerUser(request);

        assertEquals(UserService.SUCCESS_REGISTER, response.getMessage());
        assertNotNull(response.getUser());
        assertEquals("register@example.com", response.getUser().getEmail());
        assertEquals("ROLE_ADMIN", response.getUser().getRole());
        verify(spyService).createUser(eq(request), isNull());
        verify(corePublisherService).sendUserCreatedToCore(response);
    }

    @Test
    void registerUserFromEvent_sendsResponseAndActivatesUser() {
        RegisterRequest request = RegisterRequest.builder()
                .email("event@example.com")
                .role("ADMIN")
                .build();
        CoreMessage event = new CoreMessage();
        CoreMessage.Destination destination = new CoreMessage.Destination();
        destination.setTopic("usuario");
        event.setDestination(destination);

        Role role = Role.builder().name("ROLE_ADMIN").build();
        User saved = User.builder()
                .userId(50L)
                .email("event@example.com")
                .role(role)
                .active(false)
                .build();

        UserService spyService = org.mockito.Mockito.spy(userService);
        doReturn(saved).when(spyService).createUser(request, destination);
        when(userRepository.findById(50L)).thenReturn(Optional.of(saved));
        when(userRepository.save(saved)).thenReturn(saved);

        spyService.registerUserFromEvent(request, event);

        verify(corePublisherService).sendUserCreatedToCore(argThat(resp ->
                UserService.SUCCESS_REGISTER.equals(resp.getMessage())
                        && "event@example.com".equals(resp.getUser().getEmail())));
        assertTrue(saved.getActive());
        verify(userRepository).save(saved);
    }

    @Test
    void authenticateUser_returnsInvalidCredentialsMessageWhenLdapFails() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("auth@example.com");
        when(request.getPassword()).thenReturn("bad");

        when(ldapUserService.authenticateUser("auth@example.com", "bad")).thenReturn(false);

        LoginResponse response = userService.authenticateUser(request);

        assertNotNull(response);
        assertNull(response.getToken());
        assertTrue(response.getMessage().toLowerCase().contains("credenciales"));
        verify(ldapUserService).authenticateUser("auth@example.com", "bad");
        verifyNoInteractions(userRepository, jwtUtil);
    }

    @Test
    void authenticateUser_returnsErrorMessageWhenUserNotFound() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("auth@example.com");
        when(request.getPassword()).thenReturn("good");

        when(ldapUserService.authenticateUser("auth@example.com", "good")).thenReturn(true);
        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.empty());

        LoginResponse response = userService.authenticateUser(request);

        assertNotNull(response);
        assertNull(response.getToken());
        assertTrue(response.getMessage().contains("Error obteniendo"));

        verify(userRepository).findByEmail("auth@example.com");
        verifyNoInteractions(jwtUtil);
    }

    @Test
    void authenticateUser_returnsInactiveMessageWhenUserNotActive() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("auth@example.com");
        when(request.getPassword()).thenReturn("good");

        when(ldapUserService.authenticateUser("auth@example.com", "good")).thenReturn(true);

        Role role = Role.builder().name("ROLE_USER").build();
        User user = new User();
        user.setUserId(25L);
        user.setEmail("auth@example.com");
        user.setRole(role);
        user.setActive(false);
        user.setAddress(Collections.emptyList());

        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(user));

        LoginResponse response = userService.authenticateUser(request);

        assertNull(response.getToken());
        assertEquals(UserService.USER_NOT_ACTIVE_LOGIN, response.getMessage());
    }

    @Test
    void authenticateUser_returnsTokenAndUserInfoWhenSuccess() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("auth@example.com");
        when(request.getPassword()).thenReturn("good");

        when(ldapUserService.authenticateUser("auth@example.com", "good")).thenReturn(true);

        Role role = Role.builder().name("ROLE_USER").build();
        User user = new User();
        user.setUserId(25L);
        user.setEmail("auth@example.com");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setPhoneNumber("123");
        user.setRole(role);
        user.setActive(true);
        user.setDni("12345678");
        user.setAddress(Collections.emptyList());

        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(user));
        when(permissionService.getPermissionsForUser(25L)).thenReturn(Collections.emptyMap());
        when(jwtUtil.generateToken(any(User.class), anyMap())).thenReturn("jwt-token");

        LoginResponse response = userService.authenticateUser(request);

        assertEquals("jwt-token", response.getToken());
        assertNotNull(response.getUserInfo());
        assertEquals(25L, response.getUserInfo().getId());
        assertEquals("auth@example.com", response.getUserInfo().getEmail());
        assertEquals(UserService.SUCCESS_LOGIN, response.getMessage());

        verify(jwtUtil).generateToken(argThat(u -> "auth@example.com".equals(u.getEmail())
                && "ROLE_USER".equals(u.getRole().getName())), anyMap());
    }

    @Test
    void resetPassword_updatesTimestampWhenSuccessful() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("reset@example.com");
        request.setOldPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        User user = new User();
        user.setEmail("reset@example.com");
        user.setUpdatedAt(LocalDateTime.now().minusDays(2));

        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(ldapUserService.authenticateUser("reset@example.com", "OldPass123!")).thenReturn(true);
        when(ldapUserService.resetUserPassword(user, "NewPass123!")).thenReturn(true);

        String result = userService.resetPassword(request);

        assertEquals(UserService.SUCCESS_PWD_RESET, result);
        assertTrue(user.getUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(1)));
        verify(ldapUserService).resetUserPassword(user, "NewPass123!");
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_returnsErrorWhenLdapFails() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("reset@example.com");
        request.setOldPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        User user = new User();
        user.setEmail("reset@example.com");

        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(ldapUserService.authenticateUser("reset@example.com", "OldPass123!")).thenReturn(true);
        when(ldapUserService.resetUserPassword(user, "NewPass123!")).thenReturn(false);

        String result = userService.resetPassword(request);

        assertEquals(UserService.ERROR_PWD_RESET, result);
        verify(ldapUserService).resetUserPassword(user, "NewPass123!");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_whenLdapThrows_propagatesException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("reset@example.com");
        request.setOldPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        User user = new User();
        user.setEmail("reset@example.com");

        when(userRepository.findByEmail("reset@example.com")).thenReturn(Optional.of(user));
        when(ldapUserService.authenticateUser("reset@example.com", "OldPass123!")).thenReturn(true);
        doThrow(new RuntimeException("ldap boom"))
                .when(ldapUserService).resetUserPassword(user, "NewPass123!");

        assertThrows(RuntimeException.class, () -> userService.resetPassword(request));
        verify(ldapUserService).resetUserPassword(user, "NewPass123!");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_whenUserMissing_throws() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("missing@example.com");
        request.setOldPassword("OldPass123!");
        request.setNewPassword("NewPass123!");

        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.resetPassword(request));
        assertTrue(ex.getMessage().contains("missing@example.com"));
    }

    @Test
    void resetPassword_whenNewPasswordInvalid_throwsBadRequest() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setEmail("user@example.com");
        request.setOldPassword("OldPass123!");
        request.setNewPassword("short");

        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> userService.resetPassword(request));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        verify(ldapUserService, never()).authenticateUser(any(), any());
    }

    @Test
    void updateUserPartially_updatesFieldsAndSyncsLdap() {
        Long userId = 15L;
        User existing = new User();
        existing.setUserId(userId);
        existing.setEmail("old@example.com");
        existing.setFirstName("Old");
        existing.setLastName("Name");
        existing.setPhoneNumber("111");
        existing.setDni("123");
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_USER");
        existing.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(ldapUserService.updateUserInLdap("old@example.com", existing)).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("old@example.com", null, List.of())
        );

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");
        request.setFirstName("New");
        request.setPhoneNumber("222");

        String result = userService.updateUserPartially(userId, request);

        assertEquals(UserService.SUCCESS_USER_UPDATE, result);
        assertEquals("new@example.com", existing.getEmail());
        assertEquals("New", existing.getFirstName());
        assertEquals("222", existing.getPhoneNumber());
        verify(userRepository).save(existing);
        verify(ldapUserService).updateUserInLdap("old@example.com", existing);
    }

    @Test
    void updateUserPartially_doesNotChangeRoleOrDni() {
        Long userId = 25L;
        Role role = new Role();
        role.setId(9L);
        role.setName("ROLE_CLIENTE");

        User existing = new User();
        existing.setUserId(userId);
        existing.setEmail("user@example.com");
        existing.setDni("11112222");
        existing.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(ldapUserService.updateUserInLdap("user@example.com", existing)).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null, List.of())
        );

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("Updated");

        userService.updateUserPartially(userId, request);

        assertEquals("ROLE_CLIENTE", existing.getRole().getName());
        assertEquals("11112222", existing.getDni());
        verify(userRepository).save(existing);
    }

    @Test
    void updateUserPartially_throwsWhenEmailAlreadyExists() {
        Long userId = 30L;
        Role role = new Role();
        role.setName("ROLE_USER");

        User existing = new User();
        existing.setUserId(userId);
        existing.setEmail("current@example.com");
        existing.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("current@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.existsByEmail("new@example.com")).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("current@example.com", null, List.of())
        );

        UpdateUserRequest request = new UpdateUserRequest();
        request.setEmail("new@example.com");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.updateUserPartially(userId, request));
        assertTrue(ex.getMessage().contains("ya se encuentra registrado"));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserPartially_withPassword_throwsWhenLdapUpdateFails() {
        Long userId = 40L;
        Role role = new Role();
        role.setName("ROLE_USER");

        User existing = new User();
        existing.setUserId(userId);
        existing.setEmail("user@example.com");
        existing.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(ldapUserService.updateUserInLdapWithNewPwd("user@example.com", existing, "NewPass123!"))
                .thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user@example.com", null, List.of())
        );

        UpdateUserRequest request = new UpdateUserRequest();
        request.setPassword("NewPass123!");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.updateUserPartially(userId, request));
        assertTrue(ex.getMessage().toLowerCase().contains("ldap"));
        verify(ldapUserService).updateUserInLdapWithNewPwd("user@example.com", existing, "NewPass123!");
    }

    @Test
    void updateUserPartially_throwsWhenLdapFails() {
        Long userId = 15L;
        User existing = new User();
        existing.setUserId(userId);
        existing.setEmail("old@example.com");
        existing.setFirstName("Old");
        Role role = new Role();
        role.setId(1L);
        role.setName("ROLE_USER");
        existing.setRole(role);

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.findByEmail("old@example.com")).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(ldapUserService.updateUserInLdap("old@example.com", existing)).thenReturn(false);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("old@example.com", null, List.of())
        );

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("New");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.updateUserPartially(userId, request));
        assertTrue(ex.getMessage().toLowerCase().contains("ldap"));

        verify(userRepository).save(existing);
        verify(ldapUserService).updateUserInLdap("old@example.com", existing);
    }

    @Test
    void authenticateUser_throwsWhenTokenGenerationFails() {
        LoginRequest request = mock(LoginRequest.class);
        when(request.getEmail()).thenReturn("auth@example.com");
        when(request.getPassword()).thenReturn("good");

        when(ldapUserService.authenticateUser("auth@example.com", "good")).thenReturn(true);

        Role role = Role.builder().name("ROLE_USER").build();
        User user = new User();
        user.setUserId(25L);
        user.setEmail("auth@example.com");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setPhoneNumber("123");
        user.setRole(role);
        user.setActive(true);
        user.setDni("12345678");
        user.setAddress(Collections.emptyList());

        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(user));
        when(permissionService.getPermissionsForUser(25L)).thenReturn(Collections.emptyMap());
        when(jwtUtil.generateToken(any(User.class), anyMap()))
                .thenThrow(new RuntimeException("jwt error"));

        assertThrows(RuntimeException.class, () -> userService.authenticateUser(request));
    }
}
