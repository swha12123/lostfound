package com.example.lostfound.domain.entity;

import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "lost_item")
public class LostItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LostItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LostItemStatus status;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String locationName;
    private Double latitude;
    private Double longitude;

    @Column(name = "is_approved", nullable = false)
    private boolean approved = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private Member approvedBy;

    private LocalDateTime approvedAt;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "lostItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<LostItemImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "lostItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Comment> comments = new ArrayList<>();

    public static LostItem create(LostItemCategory category, String title, String description,
                                  String locationName, Double latitude, Double longitude) {
        LostItem item = new LostItem();
        item.setCategory(category);
        item.setStatus(category.getDefaultStatus());
        item.setTitle(title);
        item.setDescription(description);
        item.setLocationName(locationName);
        item.setLatitude(latitude);
        item.setLongitude(longitude);
        return item;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public void approve(Member admin) {
        this.approved = true;
        this.approvedBy = admin;
        this.approvedAt = LocalDateTime.now();
    }

    public void markResolved() {
        this.status = LostItemStatus.RESOLVED;
    }

    public void restoreProgressStatus() {
        this.status = this.category.getDefaultStatus();
    }

    public void addImage(LostItemImage image) {
        this.images.add(image);
        image.setLostItem(this);
    }

    public void addComment(Comment comment) {
        this.comments.add(comment);
        comment.setLostItem(this);
    }
}
