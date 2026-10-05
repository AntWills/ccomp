package com.ccomp.br.domain.events.activities.utils;

import com.ccomp.br.domain.events.activities.dto.CheckInDTO;
import com.ccomp.br.domain.events.activities.persistence.checkin.CheckIn;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface CheckInMapper {
    @Mapping(source = "activity.id", target = "activityId")
    CheckInDTO checkInToCheckInDTO(CheckIn checkIn);
}
