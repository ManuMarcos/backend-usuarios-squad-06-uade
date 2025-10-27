package com.reparaya.users.service;

import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ldap.core.LdapTemplate;

import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LdapUserServiceTest {

    @Mock
    private LdapTemplate ldapTemplate;

    @InjectMocks
    private LdapUserService ldapUserService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("user@example.com")
                .firstName("John")
                .lastName("Doe")
                .role(Role.builder().id(1L).name("ROLE_USER").build())
                .build();
    }

    @Test
    void createUserInLdapBindsExpectedAttributes() throws NamingException {
        ldapUserService.createUserInLdap(user, "secret");

        ArgumentCaptor<Name> dnCaptor = ArgumentCaptor.forClass(Name.class);
        ArgumentCaptor<Attributes> attrsCaptor = ArgumentCaptor.forClass(Attributes.class);

        verify(ldapTemplate).bind(dnCaptor.capture(), isNull(), attrsCaptor.capture());
        assertThat(dnCaptor.getValue().toString()).contains("uid=user@example.com");
        Attributes attrs = attrsCaptor.getValue();
        assertThat(attrs.get("uid").get()).isEqualTo(user.getEmail());
        assertThat(attrs.get("cn").get()).isEqualTo("John Doe");
        assertThat(attrs.get("sn").get()).isEqualTo("Doe");
        assertThat(attrs.get("mail").get()).isEqualTo(user.getEmail());
        assertThat(attrs.get("userPassword").get()).isEqualTo("secret");
    }

    @Test
    void createUserInLdapWrapsExceptions() {
        doThrow(new RuntimeException("ldap down"))
                .when(ldapTemplate).bind(any(Name.class), isNull(), any(Attributes.class));

        assertThatThrownBy(() -> ldapUserService.createUserInLdap(user, "secret"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Error al crear usuario en LDAP");
    }

    @Test
    void userExistsInLdapReturnsTrueWhenLookupSucceeds() {
        when(ldapTemplate.lookup(any(Name.class))).thenReturn(new Object());

        boolean exists = ldapUserService.userExistsInLdap(user.getEmail());

        assertThat(exists).isTrue();
    }

    @Test
    void userExistsInLdapReturnsFalseWhenLookupThrows() {
        when(ldapTemplate.lookup(any(Name.class))).thenThrow(new RuntimeException("not found"));

        boolean exists = ldapUserService.userExistsInLdap(user.getEmail());

        assertThat(exists).isFalse();
    }

    @Test
    void authenticateUserReturnsTrueWhenCredentialsValid() {
        when(ldapTemplate.lookup(argThat((Name name) ->
                name != null && name.toString().contains("uid=user@example.com"))))
                .thenReturn(new Object());
        when(ldapTemplate.authenticate("ou=users", "(uid=user@example.com)", "secret")).thenReturn(true);

        boolean authenticated = ldapUserService.authenticateUser(user.getEmail(), "secret");

        assertThat(authenticated).isTrue();
    }

    @Test
    void authenticateUserReturnsFalseWhenUserMissing() {
        when(ldapTemplate.lookup(any(Name.class))).thenThrow(new RuntimeException("not found"));

        boolean authenticated = ldapUserService.authenticateUser(user.getEmail(), "secret");

        assertThat(authenticated).isFalse();
        verify(ldapTemplate, never()).authenticate(anyString(), anyString(), anyString());
    }

    @Test
    void authenticateUserReturnsFalseWhenPasswordInvalid() {
        when(ldapTemplate.lookup(any(Name.class))).thenReturn(new Object());
        when(ldapTemplate.authenticate("ou=users", "(uid=user@example.com)", "secret")).thenReturn(false);

        boolean authenticated = ldapUserService.authenticateUser(user.getEmail(), "secret");

        assertThat(authenticated).isFalse();
    }

    @Test
    void resetUserPasswordUpdatesPassword() {
        when(ldapTemplate.lookup(any(Name.class))).thenReturn(new Object());
        when(ldapTemplate.authenticate("ou=users", "(uid=user@example.com)", "newPass")).thenReturn(true);

        boolean updated = ldapUserService.resetUserPassword(user, "newPass");

        assertThat(updated).isTrue();
        ArgumentCaptor<Name> dnCaptor = ArgumentCaptor.forClass(Name.class);
        ArgumentCaptor<ModificationItem[]> modsCaptor = ArgumentCaptor.forClass(ModificationItem[].class);
        verify(ldapTemplate).modifyAttributes(dnCaptor.capture(), modsCaptor.capture());
        assertThat(dnCaptor.getValue().toString()).contains("uid=user@example.com");
        ModificationItem[] mods = modsCaptor.getValue();
        assertThat(mods).anySatisfy(item ->
                assertThat(item.getAttribute().get()).isEqualTo("newPass"));
    }

    @Test
    void resetUserPasswordReturnsFalseWhenUserDoesNotExist() {
        when(ldapTemplate.lookup(any(Name.class))).thenThrow(new RuntimeException("missing"));

        boolean updated = ldapUserService.resetUserPassword(user, "newPass");

        assertThat(updated).isFalse();
        verify(ldapTemplate, never()).modifyAttributes(any(Name.class), any());
    }

    @Test
    void updateUserInLdapWithNewPwdRenamesAndUpdates() {
        when(ldapTemplate.lookup(argThat((Name name) ->
                name != null && name.toString().contains("uid=old@example.com"))))
                .thenReturn(new Object());
        when(ldapTemplate.lookup(argThat((Name name) ->
                name != null && name.toString().contains("uid=new@example.com"))))
                .thenThrow(new RuntimeException("missing"));

        User updatedUser = User.builder()
                .email("new@example.com")
                .firstName("Jane")
                .lastName("Smith")
                .role(user.getRole())
                .build();

        boolean result = ldapUserService.updateUserInLdapWithNewPwd("old@example.com", updatedUser, "pwd123");

        assertThat(result).isTrue();
        verify(ldapTemplate).rename(any(Name.class), any(Name.class));
        ArgumentCaptor<Name> modifyDn = ArgumentCaptor.forClass(Name.class);
        verify(ldapTemplate).modifyAttributes(modifyDn.capture(), any(ModificationItem[].class));
        assertThat(modifyDn.getValue().toString()).contains("uid=new@example.com");
    }

    @Test
    void updateUserInLdapReturnsFalseWhenOldUserMissing() {
        when(ldapTemplate.lookup(any(Name.class))).thenThrow(new RuntimeException("missing"));

        boolean result = ldapUserService.updateUserInLdap("old@example.com", user);

        assertThat(result).isFalse();
        verify(ldapTemplate, never()).rename(any(Name.class), any(Name.class));
    }
}
