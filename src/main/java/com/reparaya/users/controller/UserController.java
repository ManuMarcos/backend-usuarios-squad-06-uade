package com.reparaya.users.controller;

import java.util.List;

import com.reparaya.users.dto.*;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.reparaya.users.service.UserService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
@Tag(name = "user-controller", description = "Operaciones del módulo de Usuarios")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    public static final String UPDATE_USER_ERROR = "Error al actualizar el usuario. ";
    private final UserService userService;

    @Operation(
    summary = "Listar usuarios",
    description = "Retorna el listado completo de usuarios.",
    responses = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = com.reparaya.users.dto.UserDto.class)))
        }
    )
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<UserDto> users = userService.getAllUsersDto();
        return ResponseEntity.ok(users);
    }

    @Operation(
    summary = "Obtener usuario por ID",
    parameters = {
        @Parameter(name = "id", description = "ID del usuario", example = "42")
    },
    responses = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = com.reparaya.users.dto.UserDto.class))),
        @ApiResponse(responseCode = "404", description = "No encontrado")
        }   
    )
    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUserById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserDtoById(id));
    }

    @Operation(
            summary = "Registrar usuario (público)",
            description = "Crea un nuevo usuario en el sistema.",
            security = {}, // <- sin JWT para este endpoint
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = RegisterRequest.class),
                            examples = @ExampleObject(value = "{\n" +
                                    "  \"email\":\"john.smith@mail.com\",\n" +
                                    "  \"password\":\"S3gur4!\",\n" +
                                    "  \"firstName\":\"John\",\n" +
                                    "  \"lastName\":\"Smith\",\n" +
                                    "  \"dni\":\"42233698\",\n" +
                                    "  \"phoneNumber\":\"+54 9 11 1111 2222\",\n" +
                                    "  \"role\":\"ADMIN\",\n" +
                                    "  \"address\": [\n" +
                                    "    {\n" +
                                    "      \"state\": \"Buenos Aires\",\n" +
                                    "      \"city\": \"Avellaneda\",\n" +
                                    "      \"street\": \"calle\",\n" +
                                    "      \"number\": \"123\",\n" +
                                    "      \"floor\": \"2\",\n" +
                                    "      \"apartment\": \"B\"\n" +
                                    "    },\n" +
                                    "    {\n" +
                                    "      \"state\": \"Buenos Aires\",\n" +
                                    "      \"city\": \"Quilmes\",\n" +
                                    "      \"street\": \"siempreviva\",\n" +
                                    "      \"number\": \"321\"\n" +
                                    "    }\n" +
                                    "  ]\n" +
                                    "}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "201", description = "Creado",
                            content = @Content(schema = @Schema(implementation = RegisterResponse.class))),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                    @ApiResponse(responseCode = "409", description = "Email ya registrado")
            }
    )
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> registerUser(@RequestBody @Valid RegisterRequest request) {
        try {
            RegisterResponse response = userService.registerUser(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(RegisterResponse.builder().message(e.getMessage()).build());
        }
    }

    @Operation(
            summary = "Login (público)",
            description = "Autentica al usuario y retorna el JWT.",
            security = {},
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LoginRequest.class),
                            examples = @ExampleObject(value = "{\n  \"email\":\"jane.doe@mail.com\",\n  \"password\":\"P4ssw0rd!\"\n}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "OK",
                            content = @Content(schema = @Schema(implementation = LoginResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
                    @ApiResponse(responseCode = "500", description = "Error interno")
            }
    )
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> loginUser(@RequestBody @Valid LoginRequest request) {
        try {
            LoginResponse response = userService.authenticateUser(request);

            if (response.getToken() == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new LoginResponse(null, null, "Error interno del servidor"));
        }
    }

    @Operation(
            summary = "Resetear contraseña",
            description = "Actualiza la contraseña del usuario indicado.",
            parameters = @Parameter(name = "userId", description = "ID del usuario", example = "42"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ResetPasswordRequest.class),
                            examples = @ExampleObject(value = "{\n  \"newPassword\": \"Nuev4P4ss!\"\n}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Contraseña cambiada",
                            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Contraseña cambiada con éxito"))),
                    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
            }
    )
    @PatchMapping("/{userId}/reset-password")
    public ResponseEntity<String> resetPassword(@PathVariable Long userId, @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(userService.resetPassword(userId, request.getNewPassword()));
    }

    @Operation(
            summary = "Actualizar datos de usuario",
            description = "Actualización parcial de un usuario existente.",
            parameters = {
                    @Parameter(name = "userId", description = "ID del usuario", example = "42")
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = UpdateUserRequest.class),
                            examples = @ExampleObject(value = "{\n  \"firstName\": \"Jane\",\n  \"phoneNumber\": \"+54 11 5555 6666\"\n}"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Actualizado",
                            content = @Content(mediaType = "text/plain", schema = @Schema(example = "Usuario actualizado con éxito"))),
                    @ApiResponse(responseCode = "404", description = "No encontrado")
            }
    )
    @PatchMapping("/{userId}")
    public ResponseEntity<String> updateUser(
            @PathVariable Long userId,
            @RequestBody @Valid UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUserPartially(userId, request));
    }

    @Operation(
            summary = "Activar/Inactivar usuario",
            description = "Cambia el estado 'active' del usuario.",
            parameters = @Parameter(name = "userId", description = "ID del usuario", example = "42"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = java.util.Map.class),
                            examples = @ExampleObject(value = "{ \"active\": false }"))
            ),
            responses = {
                    @ApiResponse(responseCode = "200", description = "Estado actualizado"),
                    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
            }
    )
    @PatchMapping("/{userId}/active")
    public ResponseEntity<Void> changeActiveUser(
            @PathVariable Long userId,
            @RequestBody @Valid UserChangeActiveRequest request) {
        userService.changeUserIsActive(userId, request);
        return ResponseEntity.ok().build();
    }

}
