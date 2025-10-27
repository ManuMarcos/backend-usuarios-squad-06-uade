package com.reparaya.users.util;

import com.reparaya.users.dto.AddressInfo;
import com.reparaya.users.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidatorsTest {

    @Test
    void validateRequest_AllFieldsPresent_DoesNotThrow() {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@example.com")
                .password("secret")
                .firstName("John")
                .lastName("Doe")
                .dni("12345678")
                .phoneNumber("555")
                .role("CLIENTE")
                .address(List.of(AddressInfo.builder().city("C").state("S").street("Main").number("10").build()))
                .build();

        assertThatCode(() -> Validators.validateRequest(request)).doesNotThrowAnyException();
    }

    @Test
    void validateRequest_NullRequestThrowsBadRequest() {
        assertThatThrownBy(() -> Validators.validateRequest(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Request inv");
    }

    @Test
    void validateRequest_InvalidEmailCollectsErrors() {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email")
                .password(" ")
                .firstName("")
                .lastName(null)
                .dni(" ")
                .phoneNumber(null)
                .role("")
                .build();

        assertThatThrownBy(() -> Validators.validateRequest(request))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Formato de email inv");
    }
}
