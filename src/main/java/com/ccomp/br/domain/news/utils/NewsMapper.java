package com.ccomp.br.domain.news.utils;

import com.ccomp.br.domain.news.dto.NewsResponse;
import com.ccomp.br.domain.news.dto.NewsUpdateDto;
import com.ccomp.br.domain.news.persistence.News;
import org.mapstruct.*;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        collectionMappingStrategy = CollectionMappingStrategy.SETTER_PREFERRED
)
public interface NewsMapper {
    @Mapping(target = "coverImageUrl", expression = "java(news.getCoverImageKey() == null ? null : \"/api/news/\" + news.getId() + \"/images/cover\")")
    NewsResponse newsToNewsResponse(News news);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "authorId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "slug", ignore = true)
    @Mapping(target = "blocks", ignore = true)
    void updateEntityFromDto(NewsUpdateDto dto, @MappingTarget News entity);
}
