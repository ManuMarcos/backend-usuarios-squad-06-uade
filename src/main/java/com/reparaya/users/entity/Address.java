package com.reparaya.users.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "addresses")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Address {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long addressId;

    @Column(nullable = false)
    private String state;

    @Column(nullable = false)
    private String city;

    private String locality;

    @Column(nullable = false)
    private String street;

    @Column(nullable = false)
    private String number;

    private String floor;

    private String apartment;

    @Column(name = "postal_code")
    private String postalCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_address_user"))
    private User user;
}
