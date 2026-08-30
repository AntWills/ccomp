package com.ccomp.br.domain.audit.persistence;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import com.ccomp.br.domain.audit.dto.AuditLogCursor;
import com.ccomp.br.domain.audit.dto.AuditLogResponseView;
import com.ccomp.br.domain.audit.dto.AuditLogSearchFilter;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class AuditLogBlaze {
    private final EntityManager em;
    private final CriteriaBuilderFactory cbf;
    private final EntityViewManager evm;

    public AuditLogBlaze(EntityManager em, CriteriaBuilderFactory cbf, EntityViewManager evm) {
        this.em = em;
        this.cbf = cbf;
        this.evm = evm;
    }

    @Transactional(readOnly = true)
    public List<AuditLogResponseView> findByCursor(AuditLogSearchFilter filter, AuditLogCursor cursor, int limit) {

        // 1. Cria a consulta base fluente
        var cb = cbf.create(em, AuditLog.class, "log")
                .orderByDesc("log.timestamp")
                .orderByDesc("log.id")
                .setMaxResults(limit);

        // 2. Condicionais diretas (Substitui totalmente as Specifications!)
        filter.optActorId().ifPresent(id -> cb.where("log.actorId").eq(id));
        filter.optTargetId().ifPresent(id -> cb.where("log.targetId").eq(id));
        filter.optAction().ifPresent(action -> cb.where("log.action").eq(action));
        filter.optStartDate().ifPresent(date -> cb.where("log.timestamp").ge(date));
        filter.optEndDate().ifPresent(date -> cb.where("log.timestamp").le(date));

        // 3. Condição do Cursor
        if (cursor != null && cursor.timestamp() != null && cursor.id() != null) {
            cb.whereOr()
                        .where("log.timestamp").lt(cursor.timestamp())
                        .whereAnd()
                            .where("log.timestamp").eq(cursor.timestamp())
                            .where("log.id").lt(cursor.id())
                        .endAnd()
                    .endOr();
        }

        // 4. Executa aplicando a projeção para a EntityView
        EntityViewSetting<AuditLogResponseView, ?> setting = EntityViewSetting.create(AuditLogResponseView.class);

        // 5. Retorna mapeado para o seu Record DTO original
        return evm.applySetting(setting, cb).getResultList();
    }
}
