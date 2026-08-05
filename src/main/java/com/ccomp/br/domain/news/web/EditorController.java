package com.ccomp.br.domain.news.web;

import com.ccomp.br.domain.news.application.NewsEditorServices;
import com.ccomp.br.module.email.EmailAddress;
import com.ccomp.br.shared.dto.MessageResponse;
import com.ccomp.br.shared.dto.UserDTO;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Notícias")
@RestController
@RequestMapping("api/news")
public class EditorController {
    private final NewsEditorServices newsEditorServices;

    public EditorController(NewsEditorServices newsEditorServices) {
        this.newsEditorServices = newsEditorServices;
    }

    @GetMapping("/{newsId}/editors")
    public ResponseEntity<List<UserDTO>> getEditors(
            @PathVariable Long newsId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity.ok(newsEditorServices.listEditors(UUID.fromString(jwt.getSubject()), newsId));
    }

    @PostMapping("/{newsId}/editors/{email}")
    public ResponseEntity<MessageResponse> addEditor(
            @PathVariable Long newsId,
            @PathVariable String email,
            @AuthenticationPrincipal Jwt jwt) {
        newsEditorServices.addEditor(UUID.fromString(jwt.getSubject()), new EmailAddress(email), newsId);
        return ResponseEntity.ok(new MessageResponse("Editor adicionado com sucesso."));
    }

    @DeleteMapping("/{newsId}/editors/{email}")
    public ResponseEntity<MessageResponse> removeEditor(
            @PathVariable Long newsId,
            @PathVariable String email,
            @AuthenticationPrincipal Jwt jwt) {
        newsEditorServices.removeEditor(UUID.fromString(jwt.getSubject()), new EmailAddress(email), newsId);
        return ResponseEntity.ok(new MessageResponse("Editor removido com sucesso."));
    }
}
