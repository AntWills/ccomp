package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.news.persistence.editor.NewsEditorsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsAccessPolicyTest {
    @Mock NewsRepository newsRepository;
    @Mock NewsEditorsRepository editorsRepository;
    @InjectMocks NewsAccessPolicy policy;

    @Test
    void authorHasAccess() {
        UUID id = UUID.randomUUID();
        when(newsRepository.existsByIdAndAuthorId(8L, id)).thenReturn(true);
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isModeratorOrAdmin).thenReturn(false);
            assertThat(policy.hasAccess(id, 8L)).isTrue();
        }
        verifyNoInteractions(editorsRepository);
    }

    @Test
    void editorHasAccessAndUnrelatedUserDoesNot() {
        UUID id = UUID.randomUUID();
        when(newsRepository.existsByIdAndAuthorId(8L, id)).thenReturn(false);
        when(editorsRepository.existsByNewsIdAndUserId(8L, id)).thenReturn(true);
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isModeratorOrAdmin).thenReturn(false);
            assertThat(policy.hasAccess(id, 8L)).isTrue();
            when(editorsRepository.existsByNewsIdAndUserId(9L, id)).thenReturn(false);
            assertThat(policy.hasAccess(id, 9L)).isFalse();
        }
    }

    @Test
    void moderatorHasAccessWithoutRepositoryQueries() {
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isModeratorOrAdmin).thenReturn(true);
            assertThat(policy.hasAccess(UUID.randomUUID(), 1L)).isTrue();
        }
        verifyNoInteractions(newsRepository, editorsRepository);
    }
}
