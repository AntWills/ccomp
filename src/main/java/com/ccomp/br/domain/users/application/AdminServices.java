package com.ccomp.br.domain.users.application;

import com.ccomp.br.domain.security.jwt.application.JwtService;
import com.ccomp.br.domain.users.dto.*;
import com.ccomp.br.domain.users.enums.EnumActionType;
import com.ccomp.br.domain.users.enums.EnumActorType;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.external.RolesServices;
import com.ccomp.br.domain.users.persistence.UserModel;
import com.ccomp.br.domain.users.persistence.UserModelRepository;
import com.ccomp.br.domain.users.persistence.UserSpec;
import com.ccomp.br.domain.users.persistence.audit.AuditLog;
import com.ccomp.br.domain.users.persistence.audit.AuditLogRepository;
import com.ccomp.br.domain.users.persistence.audit.AuditLogSpec;
import com.ccomp.br.domain.users.persistence.audit.ChangeLog;
import com.ccomp.br.domain.users.util.UserMapper;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.DomainException;
import com.ccomp.br.shared.exceptions.UserNotFoundException;
import com.ccomp.br.shared.utils.CursorUtils;
import com.ccomp.br.shared.utils.CursorPage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
public class AdminServices {
    private final UserModelRepository userModelRepository;
    private final JwtService jwtService;
    private final UserMapper userMapper;
    private final RolesServices rolesServices;
    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AdminServices(UserModelRepository userModelRepository, JwtService jwtService, UserMapper userMapper, RolesServices rolesServices, AuditLogRepository auditLogRepository){
        this.userModelRepository = userModelRepository;
        this.jwtService = jwtService;
        this.userMapper = userMapper;
        this.rolesServices = rolesServices;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public CursorPage<UserItem> searchUsers(UserSearchFilter filter, String cursor, int pageSize){
        if(pageSize > 50) pageSize = 50;

        Specification<UserModel> spec = UserSpec.buildSpecByCursor(filter,
                        CursorUtils.decode(cursor, LocalDateTime.class).orElse(null));

        int finalPageSize = pageSize;
        List<UserModel> results = userModelRepository.findBy(spec, query -> query
                .sortBy(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(finalPageSize + 1)
                .all());

        boolean hasNext = results.size() > finalPageSize;
        List<UserModel> page = hasNext ? results.subList(0, finalPageSize) : results;

        List<UserItem> items = page.stream()
                .map(userMapper::userToItem)
                .toList();

        String nextCursor = hasNext ? CursorUtils.encode(page.getLast().getCreatedAt()) : null;

        return new CursorPage<>(items, nextCursor, null);
    }

    @Transactional(readOnly = true)
    public CursorPage<AuditLogResponse> searchAuditLogs(AuditLogSearchFilter filter, String cursor, int pageSize) {
        if(pageSize > 50) pageSize = 50;

        int finalPageSize = pageSize;

        AuditLogCursor decodedCursor = CursorUtils.decode(cursor, AuditLogCursor.class).orElse(null);

        List<AuditLogResponse> results = auditLogRepository
                .findBy(
                        AuditLogSpec
                                .buildSpec(filter, decodedCursor),
                        query -> query
                                .sortBy(Sort.by(
                                        Sort.Order.desc("timestamp"),
                                        Sort.Order.desc("id")))
                                .as(AuditLogResponse.class)
                                .limit(finalPageSize)
                                .all()
                );

        return CursorUtils.buildPage(results, finalPageSize, e -> new AuditLogCursor(e.timestamp(), e.id()));
    }

    @Transactional(readOnly = true)
    public Optional<UserDTO> getByEmail(EmailAddress email){
        log.info("Buscando no banco os dados do email: {}", email);
        return userModelRepository.findByEmailAddress(email)
                .map(userMapper::userToDto);
    }

    @Transactional
    public void blockUser(UUID userId, String reason, UUID adminId) {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário com id [%s] não encontrado.".formatted(userId)));

        jwtService.deleteRefreshTokenByUserId(userId);
        String previousStatus = user.getStatusAccount().name();
        user.block();
        String newStatus = user.getStatusAccount().name();

        var audiLog = AuditLog.builder()
                .action(EnumActionType.BLOCK.name())
                .actorType(EnumActorType.USER)
                .actorId(adminId)
                .targetId(userId)
                .reason(reason)
                .changes(
                        Map.of(
                                "status_account",
                                new ChangeLog(previousStatus, newStatus)
                        )
                )
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(audiLog);

        userModelRepository.save(user);
    }

    @Transactional
    public void unlockUser(UUID userId, String reason, UUID adminId) {
        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário com id [%s] não encontrado.".formatted(userId)));

        String previousStatus = user.getStatusAccount().name();
        user.unlock();
        String newStatus = user.getStatusAccount().name();

        var audiLog = AuditLog.builder()
                .action(EnumActionType.UNLOCK.name())
                .actorType(EnumActorType.USER)
                .actorId(adminId)
                .targetId(userId)
                .reason(reason)
                .changes(
                        Map.of(
                                "status_account",
                                new ChangeLog(previousStatus, newStatus)
                        )
                )
                .timestamp(LocalDateTime.now())
                .build();

        auditLogRepository.save(audiLog);

        userModelRepository.save(user);
    }

    @Transactional
    public void changeRole(UUID userId, EnumRoles role, UUID adminId) {
        if(userId.equals(adminId))
            throw new DomainException("O admin atual não pode alterar o próprio cargo.");

        UserModel user = userModelRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Usuário com id [%s] não encontrado.".formatted(userId)));

        var audiLog = AuditLog.builder()
                .action(EnumActionType.CHANGE_ROLE.name())
                .actorType(EnumActorType.USER)
                .actorId(adminId)
                .targetId(userId)
                .changes(
                        Map.of(
                                "role",
                                new ChangeLog(user.getRole().getRole(), role)
                        )
                )
                .timestamp(LocalDateTime.now())
                .build();

        rolesServices.changeRole(user, role);

        auditLogRepository.save(audiLog);
    }
}
