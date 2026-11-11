package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.auth.security.SecurityUtils;
import com.ccomp.br.domain.news.persistence.News;
import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.news.persistence.editor.NewsEditors;
import com.ccomp.br.domain.news.persistence.editor.NewsEditorsRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.domain.users.enums.EnumRoles;
import com.ccomp.br.domain.users.enums.EnumUserStatusAccount;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.dto.UserSummaryView;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsEditorServicesTest {
    @Mock NewsAccessPolicy accessPolicy;
    @Mock NewsRepository newsRepository;
    @Mock UserManagement userManagement;
    @Mock NewsEditorsRepository editorsRepository;
    @InjectMocks NewsEditorServices service;

    @Test
    void listEditorsChecksNewsAndAccessThenResolvesUserSummaries() {
        UUID requester = UUID.randomUUID();
        UUID editor = UUID.randomUUID();
        when(newsRepository.existsById(5L)).thenReturn(true);
        when(accessPolicy.hasAccess(requester, 5L)).thenReturn(true);
        when(editorsRepository.findAllByNewsId(5L)).thenReturn(List.of(NewsEditors.builder().userId(editor).build()));
        List<UserSummaryView> expected = List.of(mock(UserSummaryView.class));
        when(userManagement.findAllSummaryByIds(List.of(editor))).thenReturn(expected);

        assertThat(service.listEditors(requester, 5L)).isSameAs(expected);
    }

    @Test
    void listEditorsRejectsMissingNewsAndUnauthorizedRequester() {
        UUID requester = UUID.randomUUID();
        when(newsRepository.existsById(5L)).thenReturn(false);
        assertThrows(ResourceNotFoundException.class, () -> service.listEditors(requester, 5L));
        when(newsRepository.existsById(6L)).thenReturn(true);
        when(accessPolicy.hasAccess(requester, 6L)).thenReturn(false);
        assertThrows(AccessDeniedException.class, () -> service.listEditors(requester, 6L));
    }

    @Test
    void listNewsForUserCombinesAuthoredAndEditedNews() {
        UUID userId = UUID.randomUUID();
        News authored = News.builder().id(1L).build();
        News edited = News.builder().id(2L).build();
        when(editorsRepository.findAllByUserId(userId)).thenReturn(List.of(NewsEditors.builder().newsId(2L).build()));
        when(newsRepository.findAllByAuthorId(userId)).thenReturn(List.of(authored));
        when(newsRepository.findAllById(List.of(2L))).thenReturn(List.of(edited));

        var response = service.listNewsForUser(userId);

        assertThat(response.author()).containsExactly(authored);
        assertThat(response.editor()).containsExactly(edited);
    }

    @Test
    void addEditorRequiresAuthorOrModeratorAndSavesResolvedUser() {
        UUID author = UUID.randomUUID();
        UUID editor = UUID.randomUUID();
        EmailAddress email = new EmailAddress("editor@example.com");
        News news = mock(News.class);
        when(newsRepository.findById(4L)).thenReturn(Optional.of(news));
        when(news.isAuthor(author)).thenReturn(true);
        when(userManagement.findByEmailAddress(email)).thenReturn(Optional.of(mockUser(editor)));
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isModeratorOrAdmin).thenReturn(false);
            service.addEditor(author, email, 4L);
        }
        verify(editorsRepository).save(argThat(link -> link.getUserId().equals(editor) && link.getNewsId().equals(4L)));
    }

    @Test
    void removeEditorThrowsWhenNoLinkWasDeleted() {
        UUID author = UUID.randomUUID();
        EmailAddress email = new EmailAddress("editor@example.com");
        News news = mock(News.class);
        when(news.getId()).thenReturn(4L);
        when(newsRepository.findById(4L)).thenReturn(Optional.of(news));
        when(news.isAuthor(author)).thenReturn(true);
        when(userManagement.findByEmailAddress(email)).thenReturn(Optional.of(mockUser(UUID.randomUUID())));
        when(editorsRepository.deleteByUserIdAndNewsId(any(), eq(4L))).thenReturn(0L);
        try (MockedStatic<SecurityUtils> security = mockStatic(SecurityUtils.class)) {
            security.when(SecurityUtils::isModeratorOrAdmin).thenReturn(false);
            assertThrows(ResourceNotFoundException.class, () -> service.removeEditor(author, email, 4L));
        }
    }

    private UserDTO mockUser(UUID id) {
        return new UserDTO(id, "Editor", null, EnumUserStatusAccount.ACTIVE, null, null, EnumRoles.USER, null);
    }
}
