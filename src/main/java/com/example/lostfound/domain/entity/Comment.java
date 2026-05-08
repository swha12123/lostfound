package com.example.lostfound.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "item_comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lost_item_id")
    private LostItem lostItem;

    @Column(nullable = false, length = 30)
    private String authorName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String commentPassword;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Comment(String authorName, String content, String commentPassword) {
        this.authorName = authorName;
        this.content = content;
        this.commentPassword = commentPassword;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}