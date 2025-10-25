package com.reparaya.users.service;

import com.reparaya.users.dto.AddressInfo;
import com.reparaya.users.dto.LoginRequest;
import com.reparaya.users.dto.LoginResponse;
import com.reparaya.users.dto.RegisterRequest;
import com.reparaya.users.dto.RegisterResponse;
import com.reparaya.users.dto.UpdateUserRequest;
import com.reparaya.users.dto.UserChangeActiveRequest;
import com.reparaya.users.entity.Address;
import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import com.reparaya.users.repository.RoleRepository;
import com.reparaya.users.repository.UserRepository;
import com.reparaya.users.util.JwtUtil;
import com.reparaya.users.util.RegisterOriginEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Collections;
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
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private LdapUserService ldapUserService;
    @Mock private JwtUtil jwtUtil;
    @Mock private RoleRepository roleRepository;

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
    void createUser_persistsUserAndCreatesLdap() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getEmail()).thenReturn("new@example.com");
        when(request.getRole()).thenReturn(" admin ");
        when(request.getFirstName()).thenReturn("John");
        when(request.getLastName()).thenReturn("Doe");
        when(request.getPhoneNumber()).thenReturn("+54 11 1234 5678");
        when(request.getDni()).thenReturn("12345678");
        when(request.getPassword()).thenReturn("Secret123");
        AddressInfo primary = AddressInfo.builder()
                .state("Buenos Aires")
                .city("Avellaneda")
                .street("Belgrano")
                .number("123")
                .postalCode("1870")
                .build();
        when(request.getPrimaryAddressInfo()).thenReturn(primary);
        when(request.getSecondaryAddressInfo()).thenReturn(null);

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(ldapUserService.userExistsInLdap("new@example.com")).thenReturn(false);
        Role role = Role.builder().id(1L).name("ROLE_ADMIN").description("Admin").active(true).build();
        when(roleRepository.findByName("ADMIN")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(42L);
            return user;
        });

        User created = userService.createUser(request);

        assertNotNull(created);
        assertEquals(42L, created.getUserId());
        assertEquals("new@example.com", created.getEmail());
        assertEquals(1, created.getAddresses().size());
        Address storedAddress = created.getAddresses().get(0);
        assertEquals("Avellaneda", storedAddress.getCity());
        assertEquals(created, storedAddress.getUser());

        verify(userRepository).existsByEmail("new@example.com");
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
                eq("Secret123")
        );
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void createUser_whenEmailExists_throwsExceptionAndSkipsLdap() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getEmail()).thenReturn("duplicate@example.com");

        when(userRepository.existsByEmail("duplicate@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("duplicate@example.com"));

        verify(userRepository).existsByEmail("duplicate@example.com");
        verifyNoInteractions(ldapUserService);
        verify(userRepository, never()).save(any(User.class));
        verify(roleRepository, never()).findByName(any());
    }

    @Test
    void createUser_whenRoleDoesNotExist_throwsException() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getEmail()).thenReturn("role-missing@example.com");
        when(request.getRole()).thenReturn("manager");

        when(userRepository.existsByEmail("role-missing@example.com")).thenReturn(false);
        when(ldapUserService.userExistsInLdap("role-missing@example.com")).thenReturn(false);
        when(roleRepository.findByName("MANAGER")).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("Rol no encontrado"));

        verify(userRepository).existsByEmail("role-missing@example.com");
        verify(ldapUserService).userExistsInLdap("role-missing@example.com");
        verify(roleRepository).findByName("MANAGER");
        verify(userRepository, never()).save(any(User.class));
        verify(ldapUserService, never()).createUserInLdap(any(User.class), any());
    }

    @Test
    void createUser_whenLdapCreationFails_rollsBackAndThrows() {
        RegisterRequest request = mock(RegisterRequest.class);
        when(request.getEmail()).thenReturn("rollback@example.com");
        when(request.getRole()).thenReturn("user");
        when(request.getFirstName()).thenReturn("Jane");
        when(request.getLastName()).thenReturn("Doe");
        when(request.getPhoneNumber()).thenReturn("111");
        when(request.getDni()).thenReturn("999");
        when(request.getPassword()).thenReturn("Secret");
        AddressInfo primary = AddressInfo.builder()
                .state("BA")
                .city("Lanus")
                .street("Ituzaingo")
                .number("456")
                .build();
        when(request.getPrimaryAddressInfo()).thenReturn(primary);
        when(request.getSecondaryAddressInfo()).thenReturn(null);

        when(userRepository.existsByEmail("rollback@example.com")).thenReturn(false);
        when(ldapUserService.userExistsInLdap("rollback@example.com")).thenReturn(false);
        Role role = Role.builder().id(5L).name("ROLE_USER").build();
        when(roleRepository.findByName("USER")).thenReturn(Optional.of(role));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setUserId(7L);
            return u;
        });
        doThrow(new RuntimeException("LDAP error"))
                .when(ldapUserService).createUserInLdap(any(User.class), eq("Secret"));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.createUser(request));
        assertTrue(ex.getMessage().contains("LDAP"));

        verify(userRepository).save(userCaptor.capture());
        User persisted = userCaptor.getValue();
        assertEquals("rollback@example.com", persisted.getEmail());

        verify(ldapUserService).createUserInLdap(any(User.class), eq("Secret"));
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
        doReturn(created).when(spyService).createUser(request);

        RegisterResponse response = spyService.registerUser(request);

        assertEquals(UserService.SUCCESS_REGISTER, response.getMessage());
        assertEquals("register@example.com", response.getEmail());
        assertEquals("ROLE_ADMIN", response.getRole());
        verify(spyService).createUser(request);
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
        user.setAddresses(Collections.emptyList());

        when(userRepository.findByEmail("auth@example.com")).thenReturn(Optional.of(user));
        when(jwtUtil.generateToken("auth@example.com", "ROLE_USER")).thenReturn("jwt-token");

        LoginResponse response = userService.authenticateUser(request);

        assertEquals("jwt-token", response.getToken());
        assertNotNull(response.getUserInfo());
        assertEquals(25L, response.getUserInfo().getId());
        assertEquals("auth@example.com", response.getUserInfo().getEmail());
        assertEquals(UserService.SUCCESS_LOGIN, response.getMessage());

        verify(jwtUtil).generateToken("auth@example.com", "ROLE_USER");
    }

    @Test
    void resetPassword_updatesTimestampWhenSuccessful() {
        Long userId = 44L;
        User user = new User();
        user.setUserId(userId);
        user.setEmail("reset@example.com");
        user.setUpdatedAt(LocalDateTime.now().minusDays(2));

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ldapUserService.resetUserPassword(user, "NewPass123")).thenReturn(true);

        String result = userService.resetPassword(userId, "NewPass123");

        assertEquals(UserService.SUCCESS_PWD_RESET, result);
        assertTrue(user.getUpdatedAt().isAfter(LocalDateTime.now().minusMinutes(1)));
        verify(ldapUserService).resetUserPassword(user, "NewPass123");
        verify(userRepository).save(user);
    }

    @Test
    void resetPassword_returnsErrorWhenLdapFails() {
        Long userId = 55L;
        User user = new User();
        user.setUserId(userId);
        user.setEmail("reset@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(ldapUserService.resetUserPassword(user, "NewPass123")).thenReturn(false);

        String result = userService.resetPassword(userId, "NewPass123");

        assertEquals(UserService.ERROR_PWD_RESET, result);
        verify(ldapUserService).resetUserPassword(user, "NewPass123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_whenLdapThrows_propagatesException() {
        Long userId = 60L;
        User user = new User();
        user.setUserId(userId);
        user.setEmail("reset@example.com");

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        doThrow(new RuntimeException("ldap boom"))
                .when(ldapUserService).resetUserPassword(user, "NewPass123");

        assertThrows(RuntimeException.class, () -> userService.resetPassword(userId, "NewPass123"));
        verify(ldapUserService).resetUserPassword(user, "NewPass123");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_whenUserMissing_throws() {
        Long userId = 99L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.resetPassword(userId, "pass"));
        assertTrue(ex.getMessage().contains("no existe"));
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

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(ldapUserService.updateUserInLdap("old@example.com", existing)).thenReturn(true);

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
    void updateUserPartially_throwsWhenLdapFails() {
        Long userId = 15L;
        User existing = new User();
        existing.setUserId(userId);
        existing.setEmail("old@example.com");
        existing.setFirstName("Old");

        when(userRepository.findById(userId)).thenReturn(Optional.of(existing));
        when(userRepository.save(existing)).thenReturn(existing);
        when(ldapUserService.updateUserInLdap("old@example.com", existing)).thenReturn(false);

        UpdateUserRequest request = new UpdateUserRequest();
        request.setFirstName("New");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.updateUserPartially(userId, request));
        assertTrue(ex.getMessage().toLowerCase().contains("ldap"));

        verify(userRepository).save(existing);
        verify(ldapUserService).updateUserInLdap("old@example.com", existing);
    }
}
