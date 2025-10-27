package com.reparaya.users.repository;

import com.reparaya.users.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    @Query("SELECT p FROM Permission p " +
            "JOIN p.roles r " +
            "WHERE r.id = :roleId AND p.moduleCode = :moduleCode AND p.active = true")
    List<Permission> findPermissionsByRoleIdAndModuleCode(@Param("roleId") Long roleId,
                                                          @Param("moduleCode") String moduleCode);

    @Query("SELECT p FROM Permission p " +
            "JOIN p.roles r " +
            "WHERE r.id = :roleId AND p.active = true")
    List<Permission> findPermissionsByRoleId(@Param("roleId") Long roleId);

    @Query("SELECT p FROM Permission p " +
            "WHERE p.moduleCode = :moduleCode AND p.active = true")
    List<Permission> findPermissionsByModuleCode(@Param("moduleCode") String moduleCode);
}