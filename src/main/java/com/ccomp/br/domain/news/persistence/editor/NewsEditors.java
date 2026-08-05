package com.ccomp.br.domain.news.persistence.editor;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Table(
        name = "tb_news_editors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_news_editors_userid_newsid",
                        columnNames = {"user_id", "news_id"}
                )
        })
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class NewsEditors {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "news_id", nullable = false)
    private Long newsId;
}
