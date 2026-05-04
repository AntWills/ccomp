package com.ccomp.br.domain.news.web;

import com.ccomp.br.domain.news.application.NewsApplication;
import com.ccomp.br.domain.news.dto.NewsUpdateDto;
import com.ccomp.br.domain.news.persistence.News;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/news")
public class NewsController {
    private final NewsApplication newsApplication;

    public NewsController(NewsApplication newsApplication) {
        this.newsApplication = newsApplication;
    }

    @GetMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal Jwt jwt){
        News entity = newsApplication.create(UUID.fromString(jwt.getSubject()));

        return ResponseEntity.status(HttpStatus.CREATED).body(entity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        var response = newsApplication.getById(id);

        if(response.isPresent()) {
            return ResponseEntity.status(HttpStatus.OK).body(response.get());
        }
        return ResponseEntity.notFound().build();
    }

    @PatchMapping
    public ResponseEntity<?> update(@Valid @RequestBody NewsUpdateDto dto, @AuthenticationPrincipal Jwt jwt) {
        News entity = newsApplication.update(dto, UUID.fromString(jwt.getSubject()));

        return ResponseEntity.status(HttpStatus.OK).body(entity);
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<?> publish(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        newsApplication.publish(id, UUID.fromString(jwt.getSubject()));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/home")
    public ResponseEntity<?> getHomePage() {
        return ResponseEntity.ok(newsApplication.getHomePage());
    }

}
