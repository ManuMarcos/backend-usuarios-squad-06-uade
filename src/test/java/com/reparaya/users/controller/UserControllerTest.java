package com.reparaya.users.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reparaya.users.config.JwtAuthenticationFilter;
import com.reparaya.users.dto.LoginResponse;
import com.reparaya.users.dto.RegisterResponse;
import com.reparaya.users.dto.UpdateUserRequest;
import com.reparaya.users.dto.UserChangeActiveRequest;
import com.reparaya.users.dto.UserDto;
import com.reparaya.users.dto.UserInfoLoginResponse;
import com.reparaya.users.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc(addFilters = false)
class UserControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean UserService userService;
    @MockBean JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getAllUsers_returnsOkAndDelegatesToService() throws Exception {
        when(userService.getAllUsersDto()).thenReturn(Collections.emptyList());

        mvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(userService).getAllUsersDto();
    }

    @Test
    void getUserById_whenFound_returnsUser() throws Exception {
        UserDto user = UserDto.builder()
                .userId(5L)
                .email("found@example.com")
                .firstName("Found")
                .lastName("User")
                .build();

        when(userService.getUserDtoById(5L)).thenReturn(user);

        mvc.perform(get("/api/users/{id}", 5L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("found@example.com"));

        verify(userService).getUserDtoById(5L);
    }

    @Test
    void getUserById_whenMissing_returns404() throws Exception {
        when(userService.getUserDtoById(42L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));

        mvc.perform(get("/api/users/{id}", 42L))
                .andExpect(status().isNotFound());

        verify(userService).getUserDtoById(42L);
    }

    @Test
    void registerUser_returnsOkResponse() throws Exception {
        RegisterResponse response = RegisterResponse.builder()
                .message("OK")
                .user(UserDto.builder()
                        .userId(10L)
                        .email("new@example.com")
                        .role("ROLE_ADMIN")
                        .build())
                .build();

        when(userService.registerUser(any())).thenReturn(response);

        String payload = """
                {
                  "email": "new@example.com",
                  "password": "Secret123!",
                  "firstName": "John",
                  "lastName": "Doe",
                  "dni": "12345678",
                  "phoneNumber": "123",
                  "role": "ADMIN",
                  "address": [{
                      "state": "BA",
                      "city": "Avellaneda",
                      "street": "Belgrano",
                      "number": "123"
                  }]
                }
                """;

        mvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.email").value("new@example.com"))
                .andExpect(jsonPath("$.user.role").value("ROLE_ADMIN"));

        verify(userService).registerUser(any());
    }

    @Test
    void registerUser_invalidEmail_returns400() throws Exception {
        mvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad-email\",\"password\":\"Secret123!\",\"firstName\":\"X\",\"lastName\":\"Y\",\"dni\":\"1\",\"phoneNumber\":\"2\",\"role\":\"ADMIN\",\"address\":[{\"state\":\"BA\",\"city\":\"Avellaneda\",\"street\":\"Belgrano\",\"number\":\"123\"}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void registerUser_blankPassword_returns400() throws Exception {
        mvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"valid@example.com\",\"password\":\"\",\"firstName\":\"X\",\"lastName\":\"Y\",\"dni\":\"1\",\"phoneNumber\":\"2\",\"role\":\"ADMIN\",\"address\":[{\"state\":\"BA\",\"city\":\"Avellaneda\",\"street\":\"Belgrano\",\"number\":\"123\"}]}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(userService);
    }

    @Test
    void registerUser_whenConflict_returns409() throws Exception {
        when(userService.registerUser(any()))
                .thenThrow(new IllegalArgumentException("exists"));

        mvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"duplicate@example.com\",\"password\":\"Secret123!\",\"firstName\":\"John\",\"lastName\":\"Doe\",\"dni\":\"12345678\",\"phoneNumber\":\"123\",\"role\":\"ADMIN\",\"address\":[{\"state\":\"BA\",\"city\":\"Avellaneda\",\"street\":\"Belgrano\",\"number\":\"123\"}]}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("exists"));

        verify(userService).registerUser(any());
    }

    @Test
    void loginUser_whenSuccess_returnsToken() throws Exception {
        LoginResponse response = new LoginResponse(
                "jwt-token",
                UserInfoLoginResponse.builder()
                        .id(1L)
                        .email("auth@example.com")
                        .firstName("Auth")
                        .lastName("User")
                        .address(List.of())
                        .role("ROLE_USER")
                        .isActive(true)
                        .dni("123")
                        .phoneNumber("123")
                        .build(),
                UserService.SUCCESS_LOGIN
        );

        when(userService.authenticateUser(any())).thenReturn(response);

        mvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"auth@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.message").value(UserService.SUCCESS_LOGIN));

        verify(userService).authenticateUser(any());
    }

    @Test
    void loginUser_whenUnauthorized_returns401() throws Exception {
        LoginResponse unauthorized = new LoginResponse(null, null, "Credenciales inválidas");
        when(userService.authenticateUser(any())).thenReturn(unauthorized);

        mvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"auth@example.com\",\"password\":\"bad\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));

        verify(userService).authenticateUser(any());
    }

    @Test
    void loginUser_whenException_returns500() throws Exception {
        when(userService.authenticateUser(any())).thenThrow(new RuntimeException("boom"));

        mvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"auth@example.com\",\"password\":\"secret\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Error interno del servidor"));

        verify(userService).authenticateUser(any());
    }

    @Test
    void resetPassword_returnsOk() throws Exception {
        when(userService.resetPassword(5L, "NewPass123")).thenReturn("OK");

        mvc.perform(patch("/api/users/{userId}/reset-password", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"NewPass123\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        verify(userService).resetPassword(5L, "NewPass123");
    }

    @Test
    void updateUser_returnsOk() throws Exception {
        when(userService.updateUserPartially(eq(5L), any(UpdateUserRequest.class)))
                .thenReturn("Actualizado");

        mvc.perform(patch("/api/users/{userId}", 5L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"New\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Actualizado"));

        verify(userService).updateUserPartially(eq(5L), any(UpdateUserRequest.class));
    }

    @Test
    void updateUser_whenUserNotFound_returns404() throws Exception {
        when(userService.updateUserPartially(eq(7L), any(UpdateUserRequest.class)))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "not found"));

        mvc.perform(patch("/api/users/{userId}", 7L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"firstName\":\"New\"}"))
                .andExpect(status().isNotFound());

        verify(userService).updateUserPartially(eq(7L), any(UpdateUserRequest.class));
    }

    @Test
    void patchActive_returns200_andDelegatesToService() throws Exception {
        Long userId = 7L;
        UserChangeActiveRequest req = new UserChangeActiveRequest();
        req.setActive(true);

        mvc.perform(
                patch("/api/users/{userId}/active", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req))
        ).andExpect(status().isOk());

        verify(userService).changeUserIsActive(eq(userId), any(UserChangeActiveRequest.class));
    }
}
