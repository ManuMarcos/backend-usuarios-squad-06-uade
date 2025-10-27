package com.reparaya.users.controller;

import com.reparaya.users.dto.LoginRequest;
import com.reparaya.users.dto.TokenValidationRequest;
import com.reparaya.users.dto.TokenValidationResponse;
import com.reparaya.users.service.TokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "token-controller", description = "Operaciones del módulo de Usuarios para tokens")
@RestController
@RequestMapping("/api/token")
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;

    @Operation(
            summary = "Validar token",
            description = "Valida si el token es valido.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token válido",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = com.reparaya.users.dto.TokenValidationResponse.class))),
                    @ApiResponse(responseCode = "401", description = "Token inválido o expirado",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = com.reparaya.users.dto.TokenValidationResponse.class))),
                    @ApiResponse(responseCode = "500", description = "Error interno del servidor al procesar el token",
                            content = @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = com.reparaya.users.dto.TokenValidationResponse.class)))
            },
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TokenValidationRequest.class),
                            examples = @ExampleObject(value = "{\n  \"token\":\"unTokenvalido\" \n}"))
            )
    )
    @PostMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validateToken(@RequestBody TokenValidationRequest request) {
        return tokenService.validateToken(request.getToken());
    }

}
