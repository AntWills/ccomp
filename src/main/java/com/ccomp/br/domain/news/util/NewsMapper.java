package com.ccomp.br.domain.news.util;

import com.ccomp.br.domain.news.dto.NewsUpdateDto;
import com.ccomp.br.domain.news.persistence.News;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.SETTER_PREFERRED
)
public interface NewsMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    void updateEntityFromDto(NewsUpdateDto dto, @MappingTarget News entity);
}
