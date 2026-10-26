package com.ccomp.br.domain.observability.config;

import com.ccomp.br.domain.observability.enums.InfraLogServiceName;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class InfraLogServiceNameConverter implements Converter<String, InfraLogServiceName> {
    @Override
    public InfraLogServiceName convert(String source) {
        return InfraLogServiceName.fromValue(source);
    }
}
