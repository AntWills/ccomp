package com.ccomp.br.domain.clubs.util;

import com.ccomp.br.domain.clubs.dto.ClubResponseDTO;
import com.ccomp.br.domain.clubs.dto.UpdateClubRequestDTO;
import com.ccomp.br.domain.clubs.persistence.Club;
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
