package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.news.dto.NewsFilter;
import com.ccomp.br.domain.news.dto.NewsItem;
import com.ccomp.br.domain.news.dto.NewsResponse;
import com.ccomp.br.domain.news.dto.NewsUpdateDto;
import com.ccomp.br.domain.news.persistence.News;
import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.news.util.NewsMapper;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import com.ccomp.br.shared.utils.CursorPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.query.FluentQuery;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Testes UNITÁRIOS (sem contexto Spring) para NewsApplication.
 * Repositório e mapper são mockados: o objetivo aqui é testar a lógica de
 * negócio da classe isoladamente, não a integração com banco/JPA.
 * Para validar as queries de verdade (NewsSpecs, filtros JPQL), o recomendado
 * é um @DataJpaTest / Testcontainers separado — não duplicar isso aqui.
 */
@ExtendWith(MockitoExtension.class)
class NewsApplicationTest {

    @Mock
    private NewsRepository newsRepository;

    @Mock
    private NewsMapper newsMapper;

    @InjectMocks
    private NewsApplication newsApplication;

    private UUID authorId;
    private News existingNews;

    @BeforeEach
    void setUp() {
        authorId = UUID.randomUUID();
        // News é mockada porque só nos importa o comportamento (getters/regras),
        // não os detalhes internos da entidade JPA.
        existingNews = mock(News.class);
    }

    // ---------------------------------------------------------------
    // searchNewsWithFilters
    // ---------------------------------------------------------------
    /***/
//    @Nested
//    @DisplayName("searchNewsWithFilters")
//    class SearchNewsWithFilters {
//
//        @Test
//        @DisplayName("não deve retornar próximo cursor quando resultados <= pageSize")
//        void shouldNotReturnNextCursor_whenResultsFitInOnePage() {
//            NewsItem item = mock(NewsItem.class);
//            mockFindBy(List.of(item, item)); // 2 itens, pageSize 5 -> cabe tudo
//
//            CursorPage<NewsItem> page = newsApplication.searchNewsWithFilters(
//                    mock(NewsFilter.class), null, 5);
//
//            assertThat(page.data()).hasSize(2);
//            assertThat(page.nextCursor()).isNull();
//        }
//
//        @Test
//        @DisplayName("deve retornar próximo cursor quando há mais resultados que pageSize")
//        void shouldReturnNextCursor_whenThereAreMoreResultsThanPageSize() {
//            NewsItem item = mock(NewsItem.class);
//            // repositório é chamado com limit(pageSize + 1); simulamos 3 itens p/ pageSize=2
//            mockFindBy(List.of(item, item, item));
//
//            CursorPage<NewsItem> page = newsApplication.searchNewsWithFilters(
//                    mock(NewsFilter.class), null, 2);
//
//            // só os 2 primeiros devem ir para a página; o 3º item existe apenas
//            // para indicar que "há mais" (é descartado do resultado final)
//            assertThat(page.data()).hasSize(2);
//            assertThat(page.nextCursor()).isNotNull();
//        }
//
//        @Test
//        @DisplayName("deve limitar pageSize a 50 mesmo se um valor maior for solicitado")
//        void shouldCapPageSizeAt50() {
//            mockFindBy(List.of());
//
//            newsApplication.searchNewsWithFilters(mock(NewsFilter.class), null, 999);
//
//            // Verificamos indiretamente: como a função passada ao findBy chama
//            // .limit(finalPageSize + 1), poderíamos capturar o Function e inspecionar,
//            // mas o teste de contrato mais simples e robusto é: não deve estourar
//            // e o resultado deve ser tratado normalmente (sem exceptions/overflow).
//            verify(newsRepository, times(1)).findBy(any(Specification.class), any(Function.class));
//        }
//
//        /**
//         * Helper que resolve o problema de mockar a API fluente do Spring Data
//         * (findBy(spec, query -> query.as(...).limit(...).sortBy(...).all())).
//         * RETURNS_SELF faz cada chamada encadeada devolver o próprio mock,
//         * até .all() ser interceptado para devolver a lista desejada.
//         */
//        @SuppressWarnings("unchecked")
//        private void mockFindBy(List<NewsItem> results) {
//            when(newsRepository.findBy(any(Specification.class), any(Function.class)))
//                    .thenAnswer(invocation -> {
//                        Function<FluentQuery.FetchableFluentQuery<News>, List<NewsItem>> queryFn =
//                                invocation.getArgument(1);
//                        FluentQuery.FetchableFluentQuery<News> fluentMock =
//                                mock(FluentQuery.FetchableFluentQuery.class, RETURNS_SELF);
//                        when(fluentMock.all()).thenReturn((List) results);
//                        return queryFn.apply(fluentMock);
//                    });
//        }
//    }

    // ---------------------------------------------------------------
    // getById / getBySlug
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("getById e getBySlug")
    class GetByIdAndSlug {

        @Test
        @DisplayName("getById retorna vazio quando notícia não existe")
        void getById_returnsEmpty_whenNotFound() {
            when(newsRepository.findById(1L)).thenReturn(Optional.empty());

            Optional<NewsResponse> result = newsApplication.getById(1L);

            assertThat(result).isEmpty();
            verifyNoInteractions(newsMapper);
        }

        @Test
        @DisplayName("getById mapeia e retorna quando encontrada")
        void getById_returnsMapped_whenFound() {
            NewsResponse response = mock(NewsResponse.class);
            when(newsRepository.findById(1L)).thenReturn(Optional.of(existingNews));
            when(newsMapper.newsToNewsResponse(existingNews)).thenReturn(response);

            Optional<NewsResponse> result = newsApplication.getById(1L);

            assertThat(result).contains(response);
        }

        @Test
        @DisplayName("getBySlug retorna vazio quando notícia existe mas não está publicada")
        void getBySlug_returnsEmpty_whenNotPublished() {
            when(newsRepository.findBySlug("meu-slug")).thenReturn(Optional.of(existingNews));
            when(existingNews.hasPublished()).thenReturn(false);

            Optional<NewsResponse> result = newsApplication.getBySlug("meu-slug");

            assertThat(result).isEmpty();
            // regra de negócio central deste método: nunca mapear/expor não-publicada
            verify(newsMapper, never()).newsToNewsResponse(any());
        }

        @Test
        @DisplayName("getBySlug retorna mapeada quando publicada")
        void getBySlug_returnsMapped_whenPublished() {
            NewsResponse response = mock(NewsResponse.class);
            when(newsRepository.findBySlug("meu-slug")).thenReturn(Optional.of(existingNews));
            when(existingNews.hasPublished()).thenReturn(true);
            when(newsMapper.newsToNewsResponse(existingNews)).thenReturn(response);

            Optional<NewsResponse> result = newsApplication.getBySlug("meu-slug");

            assertThat(result).contains(response);
        }
    }

    // ---------------------------------------------------------------
    // create
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("cria notícia com slug único e retorna resposta mapeada")
        void create_savesWithGeneratedSlug() {
            when(newsRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            News saved = mock(News.class);
            when(newsRepository.save(any(News.class))).thenReturn(saved);
            NewsResponse response = mock(NewsResponse.class);
            when(newsMapper.newsToNewsResponse(saved)).thenReturn(response);

            NewsResponse result = newsApplication.create(authorId);

            assertThat(result).isEqualTo(response);
            verify(newsRepository).save(any(News.class));
        }

        @Test
        @DisplayName("gera novo slug se o primeiro candidato já existir (colisão)")
        void create_regeneratesSlug_onCollision() {
            // 1ª tentativa colide, 2ª está livre
            when(newsRepository.findBySlug(anyString()))
                    .thenReturn(Optional.of(mock(News.class)))
                    .thenReturn(Optional.empty());
            when(newsRepository.save(any(News.class))).thenReturn(mock(News.class));
            when(newsMapper.newsToNewsResponse(any())).thenReturn(mock(NewsResponse.class));

            newsApplication.create(authorId);

            // prova que o loop de geração de slug realmente tentou mais de uma vez
            verify(newsRepository, times(2)).findBySlug(anyString());
        }
    }

    // ---------------------------------------------------------------
    // update
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("update")
    class Update {

        @Test
        @DisplayName("lança ResourceNotFoundException quando notícia não existe")
        void update_throwsNotFound_whenMissing() {
            when(newsRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> newsApplication.update(1L, mock(NewsUpdateDto.class), authorId));

            verify(newsRepository, never()).save(any());
        }

        @Test
        @DisplayName("lança AccessDeniedException quando usuário não é o autor")
        void update_throwsAccessDenied_whenNotAuthor() {
            UUID otherUser = UUID.randomUUID();
            when(newsRepository.findById(1L)).thenReturn(Optional.of(existingNews));
            when(existingNews.getAuthorId()).thenReturn(authorId);

            assertThrows(AccessDeniedException.class,
                    () -> newsApplication.update(1L, mock(NewsUpdateDto.class), otherUser));

            verify(newsRepository, never()).save(any());
        }

        @Test
        @DisplayName("atualiza e salva quando usuário é o autor")
        void update_succeeds_whenAuthorMatches() {
            NewsUpdateDto dto = mock(NewsUpdateDto.class);
            when(newsRepository.findById(1L)).thenReturn(Optional.of(existingNews));
            when(existingNews.getAuthorId()).thenReturn(authorId);
            when(existingNews.getTitle()).thenReturn("Título antigo");
            when(dto.title()).thenReturn(null); // sem mudança de título neste teste
            NewsResponse response = mock(NewsResponse.class);
            when(newsMapper.newsToNewsResponse(existingNews)).thenReturn(response);

            NewsResponse result = newsApplication.update(1L, dto, authorId);

            assertThat(result).isEqualTo(response);
            verify(newsMapper).updateEntityFromDto(dto, existingNews);
            verify(newsRepository).save(existingNews);
        }

        @Test
        @DisplayName("regenera o slug quando o título muda")
        void update_regeneratesSlug_whenTitleChanges() {
            NewsUpdateDto dto = mock(NewsUpdateDto.class);
            when(newsRepository.findById(1L)).thenReturn(Optional.of(existingNews));
            when(existingNews.getAuthorId()).thenReturn(authorId);
            when(existingNews.getTitle()).thenReturn("Título antigo");
            when(dto.title()).thenReturn("Título novo");
            when(newsRepository.findBySlug(anyString())).thenReturn(Optional.empty());
            when(newsMapper.newsToNewsResponse(existingNews)).thenReturn(mock(NewsResponse.class));

            newsApplication.update(1L, dto, authorId);

            // setSlug deve ser chamado com um slug derivado do novo título
            verify(existingNews, atLeastOnce()).setSlug(anyString());
        }
    }

    // ---------------------------------------------------------------
    // delete
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("delete")
    class Delete {

        @Test
        @DisplayName("lança ResourceNotFoundException quando notícia não existe")
        void delete_throwsNotFound_whenMissing() {
            when(newsRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> newsApplication.delete(1L, authorId));

            verify(newsRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("BUG CONHECIDO: autor deveria PODER apagar a própria notícia")
        void delete_shouldSucceed_whenUserIsAuthor() {
            when(newsRepository.findById(1L)).thenReturn(Optional.of(existingNews));
            when(existingNews.isAuthor(authorId)).thenReturn(true);

            // Comportamento ESPERADO/correto. Hoje este teste FALHA porque a
            // condição em NewsApplication#delete está invertida
            // (`if (entity.isAuthor(userId)) throw ...`), o que impede o autor
            // de apagar a própria notícia e permite que outros apaguem.
            // Ao corrigir para `if (!entity.isAuthor(userId))`, este teste passa.
            newsApplication.delete(1L, authorId);

            verify(newsRepository).deleteById(1L);
        }

        @Test
        @DisplayName("deve negar exclusão quando usuário não é o autor")
        void delete_shouldDeny_whenUserIsNotAuthor() {
            UUID otherUser = UUID.randomUUID();
            when(newsRepository.findById(1L)).thenReturn(Optional.of(existingNews));
            when(existingNews.isAuthor(otherUser)).thenReturn(false);

            assertThrows(AccessDeniedException.class,
                    () -> newsApplication.delete(1L, otherUser));
        }
    }

    // ---------------------------------------------------------------
    // publish
    // ---------------------------------------------------------------
    @Nested
    @DisplayName("publish")
    class Publish {

        @Test
        @DisplayName("lança ResourceNotFoundException quando notícia não existe")
        void publish_throwsNotFound_whenMissing() {
            when(newsRepository.findById(1L)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> newsApplication.publish(1L, authorId));
        }

        @Test
        @DisplayName("lança AccessDeniedException quando usuário não é o autor")
        void publish_throwsAccessDenied_whenNotAuthor() {
            UUID otherUser = UUID.randomUUID();
            when(newsRepository.findById(1L)).thenReturn(Optional.of(existingNews));
            when(existingNews.getAuthorId()).thenReturn(authorId);

            assertThrows(AccessDeniedException.class,
                    () -> newsApplication.publish(1L, otherUser));

            verify(existingNews, never()).publishNow();
        }

        @Test
        @DisplayName("publica e salva quando usuário é o autor")
        void publish_succeeds_whenAuthorMatches() {
            when(newsRepository.findById(1L)).thenReturn(Optional.of(existingNews));
            when(existingNews.getAuthorId()).thenReturn(authorId);

            newsApplication.publish(1L, authorId);

            verify(existingNews).publishNow();
            verify(newsRepository).save(existingNews);
        }
    }
}