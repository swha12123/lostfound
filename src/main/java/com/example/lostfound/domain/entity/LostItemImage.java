package com.example.lostfound.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lost_item_image")
public class LostItemImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lost_item_id")
    private LostItem lostItem;

    private String originalFileName;
    private String storedFileName;
    private String imagePath;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public LostItemImage(String originalFileName, String storedFileName, String imagePath) {
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.imagePath = imagePath;
    }
}
