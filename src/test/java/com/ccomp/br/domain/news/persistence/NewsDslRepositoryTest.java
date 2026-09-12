package com.ccomp.br.domain.news.persistence;

import com.ccomp.br.config.QueryDslConfig;
import com.ccomp.br.domain.news.dto.NewsCursor;
import com.ccomp.br.domain.news.dto.NewsItem;
import com.ccomp.br.domain.news.dto.NewsSearchFilter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import({QueryDslConfig.class, NewsDslRepository.class})
public class NewsDslRepositoryTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private NewsDslRepository newsDslRepository;

    @Test
    @DisplayName("Deve realizar a paginação baseada em cursor ordenada por publishedAt decrescente")
    void shouldPaginateNewsUsingCursorInDescendingOrder() {

        // Arranjo
        LocalDateTime now = LocalDateTime.now();

        var news1 = News.builder()
                .title("Notícia 1")
                .slug("noticia-1")
                .summary("Resumo da notícia 1")
                .coverImageUrl("https://example.com/image1.jpg")
                .featured(false)
                .authorId(java.util.UUID.randomUUID())
                .publishedAt(now.minusHours(3))
                .build();

        var news2 = News.builder()
                .title("Notícia 2")
                .slug("noticia-2")
                .summary("Resumo da notícia 2")
                .coverImageUrl("https://example.com/image2.jpg")
                .featured(true)
                .authorId(java.util.UUID.randomUUID())
                .publishedAt(now.minusHours(2))
                .build();

        var news3 = News.builder()
                .title("Notícia 3")
                .slug("noticia-3")
                .summary("Resumo da notícia 3")
                .coverImageUrl("https://example.com/image3.jpg")
                .featured(false)
                .authorId(java.util.UUID.randomUUID())
                .publishedAt(now.minusHours(1))
                .build();

        entityManager.persist(news1);
        entityManager.persist(news2);
        entityManager.persist(news3);

        entityManager.flush();
        entityManager.clear();

        NewsSearchFilter filter = new NewsSearchFilter(null);

        // Primeira página
        List<NewsItem> page1 =
                newsDslRepository.findByCursor(filter, null, 2);

        assertThat(page1).hasSize(2);
        assertThat(page1.get(0).title()).isEqualTo("Notícia 3");
        assertThat(page1.get(1).title()).isEqualTo("Notícia 2");

        // Cursor baseado no último item da primeira página
        NewsItem lastItem = page1.getLast();

        NewsCursor cursor = new NewsCursor(
                lastItem.publishedAt(),
                lastItem.id()
        );

        // Segunda página
        List<NewsItem> page2 =
                newsDslRepository.findByCursor(filter, cursor, 2);

        assertThat(page2).hasSize(1);
        assertThat(page2.getFirst().title()).isEqualTo("Notícia 1");
    }
}