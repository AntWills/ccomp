package com.ccomp.br.domain.storage.persistence;

import com.ccomp.br.domain.users.enums.EnumRoles;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_storage_role_permissions")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StorageRolePermission {
    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 25)
    private EnumRoles role;

    @Column(name = "can_upload", nullable = false)
    private boolean canUpload;

    @Column(name = "can_read_own", nullable = false)
    private boolean canReadOwn;

    @Column(name = "can_read_any", nullable = false)
    private boolean canReadAny;

    @Column(name = "can_delete_own", nullable = false)
    private boolean canDeleteOwn;

    @Column(name = "can_delete_any", nullable = false)
    private boolean canDeleteAny;
}
