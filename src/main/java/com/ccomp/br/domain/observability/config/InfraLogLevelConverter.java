package com.ccomp.br.domain.observability.config;

import com.ccomp.br.domain.observability.enums.InfraLogLevel;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class InfraLogLevelConverter implements Converter<String, InfraLogLevel> {
    @Override
    public InfraLogLevel convert(String source) {
        return InfraLogLevel.fromValue(source);
    }
}
