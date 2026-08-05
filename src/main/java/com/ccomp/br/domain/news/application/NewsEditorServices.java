package com.ccomp.br.domain.news.application;

import com.ccomp.br.domain.news.persistence.NewsRepository;
import com.ccomp.br.domain.news.persistence.editor.NewsEditors;
import com.ccomp.br.domain.news.persistence.editor.NewsEditorsRepository;
import com.ccomp.br.domain.users.external.UserManagement;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.UserDTO;
import com.ccomp.br.shared.exceptions.AccessDeniedException;
import com.ccomp.br.shared.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class NewsEditorServices {
    private final NewsAccessPolicy newsAccessPolicy;
    private final NewsRepository newsRepository;
    private final UserManagement userManagement;
    private final NewsEditorsRepository newsEditorsRepository;

    public NewsEditorServices(NewsAccessPolicy newsAccessPolicy, NewsRepository newsRepository, UserManagement userManagement, NewsEditorsRepository newsEditorsRepository) {
        this.newsAccessPolicy = newsAccessPolicy;
        this.newsRepository = newsRepository;
        this.userManagement = userManagement;
        this.newsEditorsRepository = newsEditorsRepository;
    }

    @Transactional(readOnly = true)
    public List<UserDTO> listEditors(UUID userId, Long newsId) {
        if (!newsRepository.existsById(newsId))
            throw new ResourceNotFoundException("Notícia não existe.");

        if(!newsAccessPolicy.hasAccess(userId, newsId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        return userManagement.findAllByIds(
                newsEditorsRepository.findAllByNewsId(newsId)
                        .stream()
                        .map(NewsEditors::getUserId)
                        .toList());
    }

    @Transactional
    public void addEditor(UUID ownerId, EmailAddress userEmail, Long newsId) {
        var news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("Nóticia não existe."));

        if(!news.isAuthor(ownerId))
            throw new AccessDeniedException("O usuario não tem acesso a este recurso.");

        var user = userManagement.findByEmailAddress(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario não encontrado."));


        newsEditorsRepository.save(
                NewsEditors.builder()
                        .userId(user.id())
                        .newsId(newsId)
                        .build()
        );
    }

    @Transactional
    public void removeEditor(UUID ownerId, EmailAddress userEmail, Long newsId) {
        var news = newsRepository.findById(newsId)
                .orElseThrow(() -> new ResourceNotFoundException("Nóticia não existe."));

        if(!news.isAuthor(ownerId))
            throw new AccessDeniedException("O usuário não tem acesso a este recurso.");

        var user = userManagement.findByEmailAddress(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Nenhum usuário foi encontrado para o e-mail informado."));

        long removed = newsEditorsRepository.deleteByUserIdAndNewsId(user.id(), news.getId());
        if (removed == 0) {
            throw new ResourceNotFoundException("O usuário informado não é editor desta notícia.");
        }
    }
}
