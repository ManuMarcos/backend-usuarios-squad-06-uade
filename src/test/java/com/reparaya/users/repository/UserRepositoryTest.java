package com.reparaya.users.repository;

import com.reparaya.users.entity.Role;
import com.reparaya.users.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UserRepositoryTest {

    @Autowired UserRepository userRepository;
    @Autowired RoleRepository roleRepository;

    @Test
    void findByEmail_and_existsByEmail_work() {
        Role role = Role.builder()
                .name("ROLE_ADMIN")
                .description("Admin role")
                .active(true)
                .build();
        Role savedRole = roleRepository.save(role);

        User user = new User();
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setActive(true);
        user.setRole(savedRole);
        user.setRegisterOrigin("TEST");
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("test@example.com");
        assertTrue(found.isPresent());
        assertEquals("Test", found.get().getFirstName());
        assertEquals(savedRole.getId(), found.get().getRole().getId());

        assertTrue(userRepository.existsByEmail("test@example.com"));
        assertFalse(userRepository.existsByEmail("nope@example.com"));
    }

    @Test
    void save_withDuplicateEmail_throwsDataIntegrityViolation() {
        Role role = Role.builder()
                .name("ROLE_USER")
                .description("User role")
                .active(true)
                .build();
        Role savedRole = roleRepository.save(role);

        User user = new User();
        user.setEmail("dup@example.com");
        user.setFirstName("Dup");
        user.setLastName("User");
        user.setActive(true);
        user.setRole(savedRole);
        user.setRegisterOrigin("TEST");
        userRepository.saveAndFlush(user);

        User duplicate = new User();
        duplicate.setEmail("dup@example.com");
        duplicate.setFirstName("Dup2");
        duplicate.setLastName("User2");
        duplicate.setActive(true);
        duplicate.setRole(savedRole);
        duplicate.setRegisterOrigin("TEST");

        assertThrows(DataIntegrityViolationException.class,
                () -> userRepository.saveAndFlush(duplicate));
    }
}
