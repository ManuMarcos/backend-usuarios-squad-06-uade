package com.reparaya.users.service;

import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.test.context.ActiveProfiles;

import javax.naming.ldap.LdapName;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class LdapUserServiceIntegrationTest {

    @Autowired
    private LdapUserService ldapUserService;

    @Autowired
    private LdapTemplate ldapTemplate;

    private final List<String> createdUsers = new ArrayList<>();

    @BeforeEach
    void setUp() {
        createdUsers.clear();
    }

    @AfterEach
    void tearDown() {
        createdUsers.forEach(this::deleteUserIfExists);
        createdUsers.clear();
    }

    @Test
    void createUserInLdap_persistsEntryAndAllowsAuthentication() {
        String email = uniqueEmail();
        User newUser = buildUser(email);
        String password = "Secret123!";

        deleteUserIfExists(email);
        ldapUserService.createUserInLdap(newUser, password);
        createdUsers.add(email);

        assertThat(ldapUserService.userExistsInLdap(email)).isTrue();
        assertThat(ldapUserService.authenticateUser(email, password)).isTrue();
    }

    @Test
    void resetUserPassword_updatesPasswordInDirectory() {
        String email = uniqueEmail();
        User newUser = buildUser(email);

        deleteUserIfExists(email);
        ldapUserService.createUserInLdap(newUser, "Initial123!");
        createdUsers.add(email);

        boolean updated = ldapUserService.resetUserPassword(newUser, "Updated123!");

        assertThat(updated).isTrue();
        assertThat(ldapUserService.authenticateUser(email, "Updated123!")).isTrue();
    }

    private User buildUser(String email) {
        Role role = Role.builder()
                .id(999L)
                .name("ROLE_TEST")
                .build();

        return User.builder()
                .email(email)
                .firstName("Integration")
                .lastName("User")
                .phoneNumber("555-0000")
                .dni("99999999")
                .role(role)
                .active(true)
                .build();
    }

    private void deleteUserIfExists(String email) {
        try {
            LdapName userDn = LdapNameBuilder.newInstance()
                    .add("ou", "users")
                    .add("uid", email)
                    .build();
            ldapTemplate.unbind(userDn);
        } catch (Exception ignored) {
        }
    }

    private String uniqueEmail() {
        return "it-user-" + UUID.randomUUID() + "@test.arreglaya.dev";
    }
}
