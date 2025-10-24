package com.reparaya.users.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "module_code", nullable = false, length = 100)
    private String moduleCode;

    @Column(name = "permission_name", nullable = false, unique = true, length = 150)
    private String permissionName;

    @Column(length = 255)
    private String description;

    @Column(nullable = false)
    private boolean active = true;
}
