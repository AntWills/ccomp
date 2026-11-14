package com.ccomp.br.domain.clubs.core.utils;

import com.ccomp.br.domain.clubs.core.dto.ClubResponseDTO;
import com.ccomp.br.domain.clubs.core.dto.UpdateClubRequestDTO;
import com.ccomp.br.domain.clubs.core.persistence.Club;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;


@Mapper(
        componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ClubMapper {
    ClubResponseDTO toDTO(Club club);
    void updateEntityFromDto(UpdateClubRequestDTO dto, @MappingTarget Club club);
}
