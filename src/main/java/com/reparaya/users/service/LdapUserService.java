package com.reparaya.users.service;

import com.reparaya.users.entity.User;
import com.reparaya.users.entity.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Service;

import javax.naming.NamingException;
import javax.naming.directory.*;
import javax.naming.ldap.LdapName;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import org.springframework.ldap.core.DirContextAdapter;

@Slf4j
@Service
@RequiredArgsConstructor
public class LdapUserService {

    private final LdapTemplate ldapTemplate;

    public void createUserInLdap(User user, String password) {
        try {
            LdapName userDn = LdapNameBuilder.newInstance()
                    .add("ou", "users")
                    .add("uid", user.getEmail())
                    .build();

            Attributes attributes = new BasicAttributes();

            BasicAttribute objectClassAttribute = new BasicAttribute("objectClass");
            objectClassAttribute.add("top");
            objectClassAttribute.add("person");
            objectClassAttribute.add("organizationalPerson");
            objectClassAttribute.add("inetOrgPerson");
            attributes.put(objectClassAttribute);

            attributes.put(new BasicAttribute("uid", user.getEmail()));
            attributes.put(new BasicAttribute("cn", user.getFirstName() + " " + user.getLastName()));
            attributes.put(new BasicAttribute("sn", user.getLastName()));
            attributes.put(new BasicAttribute("mail", user.getEmail()));
            attributes.put(new BasicAttribute("description", user.getRole().toString()));

            attributes.put(new BasicAttribute("userPassword", password));

            ldapTemplate.bind(userDn, null, attributes);

        } catch (Exception e) {
            log.error("Error al crear usuario en LDAP: {}", e.getMessage(), e);
            throw new RuntimeException("Error al crear usuario en LDAP: " + e.getMessage());
        }
    }

    public boolean userExistsInLdap(String email) {
        try {
            LdapName userDn = LdapNameBuilder.newInstance()
                    .add("ou", "users")
                    .add("uid", email)
                    .build();

            ldapTemplate.lookup(userDn);
            log.info("Usuario encontrado en LDAP: {}", email);
            return true;
        } catch (Exception e) {
            log.error("Error buscando usuario en LDAP: {} - {}", email, e.getMessage());
            return false;
        }
    }

    public boolean authenticateUser(String email, String password) {
        try {
            if (!userExistsInLdap(email)) {
                log.warn("Usuario no existe en LDAP: {}", email);
                return false;
            }

            boolean auth = ldapTemplate.authenticate(
                    "ou=users",
                    "(uid=" + email + ")",
                    password
            );

            if (auth) {
                log.info("Autenticación LDAP exitosa para usuario: {}", email);
                return true;
            } else {
                log.warn("Autenticación LDAP fallida para usuario: {}", email);
                return false;
            }

        } catch (Exception e) {
            log.error("Error durante autenticación LDAP para usuario {}: {}", email, e.getMessage(), e);
            return false;
        }
    }

    public boolean resetUserPassword(User user, String newPassword) {
        try {
            LdapName userDn = LdapNameBuilder.newInstance()
                    .add("ou", "users")
                    .add("uid", user.getEmail())
                    .build();
    
            if (!userExistsInLdap(user.getEmail())) {
                log.warn("No se puede cambiar contraseña: usuario no existe en LDAP: {}", user.getEmail());
                return false;
            }
    
            ModificationItem[] mods = new ModificationItem[]{
                new ModificationItem(
                    DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute("userPassword", newPassword)
                )
            };
    
            ldapTemplate.modifyAttributes(userDn, mods);
            log.info("Contraseña actualizada en LDAP para usuario: {}", user.getEmail());
    
            return ldapTemplate.authenticate("ou=users", "(uid=" + user.getEmail() + ")", newPassword);
    
        } catch (Exception e) {
            log.error("Error al actualizar la contraseña en LDAP para {}: {}", user.getEmail(), e.getMessage(), e);
            return false;
        }
    }

    public boolean updateUserInLdap(String email, User user) {
        try {
            LdapName userDn = LdapNameBuilder.newInstance()
                    .add("ou", "users")
                    .add("uid", email)
                    .build();

            if (!userExistsInLdap(email)) {
                log.warn("No se puede actualizar: usuario no existe en LDAP: {}", email);
                return false;
            }

            ModificationItem[] mods = new ModificationItem[]{
                    new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                            new BasicAttribute("cn", user.getFirstName() + " " + user.getLastName())),
                    new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                            new BasicAttribute("sn", user.getLastName())),
                    new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                            new BasicAttribute("mail", user.getEmail())),
                    new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                            new BasicAttribute("description", user.getRole().toString()))
            };

            ldapTemplate.modifyAttributes(userDn, mods);
            log.info("Usuario actualizado en LDAP: {}", user.getEmail());
            return true;

        } catch (Exception e) {
            log.error("Error al actualizar usuario en LDAP {}: {}", email, e.getMessage(), e);
            return false;
        }
    }
}

