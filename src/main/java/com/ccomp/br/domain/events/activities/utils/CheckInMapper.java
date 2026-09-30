package com.ccomp.br.domain.events.activities.utils;

import com.ccomp.br.domain.events.activities.dto.CheckInDTO;
import com.ccomp.br.domain.events.activities.persistence.checkin.CheckIn;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CheckInMapper {

    CheckInDTO checkInToCheckInDTO(CheckIn checkIn);
}
