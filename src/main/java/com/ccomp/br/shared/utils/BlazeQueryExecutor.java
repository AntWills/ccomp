package com.ccomp.br.shared.utils;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.view.EntityViewManager;
import com.blazebit.persistence.view.EntityViewSetting;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BlazeQueryExecutor {

    private final EntityViewManager evm;

    public BlazeQueryExecutor(EntityViewManager evm) {
        this.evm = evm;
    }

    // Busca de listas
    public <T> List<T> fetchList(CriteriaBuilder<?> cb, Class<T> viewClass) {
        return evm.applySetting(EntityViewSetting.create(viewClass), cb).getResultList();
    }

    // Busca de resultado único opcional
    public <T> Optional<T> fetchOptional(CriteriaBuilder<?> cb, Class<T> viewClass) {
        List<T> results = evm.applySetting(EntityViewSetting.create(viewClass), cb).getResultList();
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }
}
