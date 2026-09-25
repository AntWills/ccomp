package com.ccomp.br.domain.storage.persistence;

import com.ccomp.br.domain.users.enums.EnumRoles;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StorageRolePermissionRepository extends JpaRepository<StorageRolePermission, EnumRoles> {
}
