package com.example.lostfound.domain.entity;

import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemStatus;
import com.example.lostfound.domain.enums.LostItemType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "lost_item",
        indexes = {
                @Index(name = "idx_lost_item_approved_created", columnList = "is_approved, created_at"),
                @Index(name = "idx_lost_item_approved_category_created", columnList = "is_approved, category, created_at"),
                @Index(name = "idx_lost_item_approved_status", columnList = "is_approved, status"),
                @Index(name = "idx_lost_item_item_type", columnList = "item_type")
        }
)
public class LostItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LostItemCategory category;

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private LostItemType itemType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LostItemStatus status;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String locationName;

    @Column(length = 100)
    private String contactInfo;

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
    @BatchSize(size = 50)
    private List<LostItemImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "lostItem", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @BatchSize(size = 50)
    private List<Comment> comments = new ArrayList<>();

    public static LostItem create(LostItemCategory category, LostItemType itemType, String title, String description,
                                  String locationName, String contactInfo, Double latitude, Double longitude) {
        LostItem item = new LostItem();
        item.setCategory(category);
        item.setItemType(itemType);
        item.setStatus(category.getDefaultStatus());
        item.setTitle(title);
        item.setDescription(description);
        item.setLocationName(locationName);
        item.setContactInfo(contactInfo);
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

    public void updateDetails(LostItemCategory category, LostItemType itemType, String title, String description,
                              String locationName, String contactInfo, Double latitude, Double longitude) {
        boolean categoryChanged = this.category != category;

        this.category = category;
        this.itemType = itemType;
        this.title = title;
        this.description = description;
        this.locationName = locationName;
        this.contactInfo = contactInfo;
        this.latitude = latitude;
        this.longitude = longitude;

        if (categoryChanged && this.status != LostItemStatus.RESOLVED) {
            this.status = category.getDefaultStatus();
        }
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
