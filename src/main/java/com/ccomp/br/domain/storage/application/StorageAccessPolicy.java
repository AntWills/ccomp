package com.ccomp.br.domain.storage.application;

import com.ccomp.br.domain.storage.persistence.StorageFile;
import com.ccomp.br.domain.storage.persistence.StorageRolePermission;
import com.ccomp.br.domain.storage.persistence.StorageRolePermissionRepository;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class StorageAccessPolicy {
    private final StorageRolePermissionRepository permissions;

    public StorageAccessPolicy(StorageRolePermissionRepository permissions) {
        this.permissions = permissions;
    }

    public void requireUpload() {
        if (!permissionForCurrentUser().isCanUpload()) throw denied();
    }

    public void requireRead(StorageFile file, UUID userId) {
        StorageRolePermission permission = permissionForCurrentUser();
        if (permission.isCanReadAny()) return;
        if (permission.isCanReadOwn() && file != null && file.getOwnerUserId().equals(userId)) return;
        throw denied();
    }

    public void requireDelete(StorageFile file, UUID userId) {
        StorageRolePermission permission = permissionForCurrentUser();
        if (permission.isCanDeleteAny()) return;
        if (permission.isCanDeleteOwn() && file != null && file.getOwnerUserId().equals(userId)) return;
        throw denied();
    }

    private StorageRolePermission permissionForCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) throw denied();
        EnumRoles role = authentication.getAuthorities().stream()
                .map(authority -> authority.getAuthority().replaceFirst("^ROLE_", ""))
                .map(value -> {
                    try { return EnumRoles.valueOf(value); }
                    catch (IllegalArgumentException ignored) { return null; }
                })
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(StorageAccessPolicy::denied);
        return permissions.findById(role).orElseThrow(StorageAccessPolicy::denied);
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Você não tem permissão para acessar este arquivo.");
    }
}
