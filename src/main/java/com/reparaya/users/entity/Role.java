package com.reparaya.users.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Guardar con prefijo estándar de Spring Security: ROLE_ADMIN, ROLE_USER, etc.
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}


