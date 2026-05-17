package com.example.lostfound.service;

import com.example.lostfound.domain.entity.Comment;
import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.domain.entity.LostItemImage;
import com.example.lostfound.domain.entity.Member;
import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemStatus;
import com.example.lostfound.domain.enums.LostItemType;
import com.example.lostfound.domain.enums.Role;
import com.example.lostfound.domain.repository.CommentRepository;
import com.example.lostfound.domain.repository.LostItemImageRepository;
import com.example.lostfound.domain.repository.LostItemRepository;
import com.example.lostfound.domain.repository.MemberRepository;
import com.example.lostfound.dto.CommentCreateForm;
import com.example.lostfound.dto.CommentViewDto;
import com.example.lostfound.dto.LostItemCreateForm;
import com.example.lostfound.dto.LostItemDetailDto;
import com.example.lostfound.dto.LostItemListDto;
import com.example.lostfound.dto.LostItemStatisticsDto;
import com.example.lostfound.strategy.itemtype.LostItemTypeStrategy;
import com.example.lostfound.strategy.itemtype.LostItemTypeStrategyResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostItemService {

    private static final int BOARD_PAGE_SIZE = 5;

    private final LostItemRepository lostItemRepository;
    private final LostItemImageRepository lostItemImageRepository;
    private final CommentRepository commentRepository;
    private final FileStoreService fileStoreService;
    private final MemberRepository memberRepository;
    private final LostItemTypeStrategyResolver lostItemTypeStrategyResolver;

    @Transactional
    public Long createAnonymousItem(LostItemCreateForm form, MultipartFile imageFile) {
        LostItem item = LostItem.create(
                form.getCategory(),
                form.getItemType(),
                form.getTitle(),
                form.getDescription(),
                form.getLocationName(),
                form.getContactInfo(),
                form.getLatitude(),
                form.getLongitude()
        );

        lostItemRepository.save(item);

        if (imageFile != null && !imageFile.isEmpty()) {
            String[] storeResult = fileStoreService.storeFile(imageFile);
            LostItemImage image = LostItemImage.builder()
                    .originalFileName(imageFile.getOriginalFilename())
                    .storedFileName(storeResult[0])
                    .imagePath(storeResult[1])
                    .build();
            item.addImage(image);
            lostItemImageRepository.save(image);
        }

        return item.getId();
    }

    public List<LostItemListDto> getApprovedItems(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<LostItem> items;
        if (normalizedKeyword == null) {
            items = lostItemRepository.findByApprovedTrueOrderByIdDesc();
        } else {
            items = lostItemRepository.findByApprovedTrueAndTitleContainingOrderByIdDesc(normalizedKeyword);
        }
        return items.stream().map(this::toListDto).collect(Collectors.toList());
    }

    public Page<LostItemListDto> getApprovedItemsByCategory(LostItemCategory category,
                                                            LostItemType itemType,
                                                            String keyword,
                                                            int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), BOARD_PAGE_SIZE);
        return lostItemRepository.findApprovedPageByCategory(category, itemType, normalizeKeyword(keyword), pageable)
                .map(this::toListDto);
    }

    public LostItemStatisticsDto getApprovedStatistics() {
        long searchingCount = lostItemRepository.countByApprovedTrueAndStatus(LostItemStatus.SEARCHING);
        long foundCount = lostItemRepository.countByApprovedTrueAndStatus(LostItemStatus.FOUND);
        long resolvedCount = lostItemRepository.countByApprovedTrueAndStatus(LostItemStatus.RESOLVED);

        return LostItemStatisticsDto.builder()
                .searchingCount(searchingCount)
                .foundCount(foundCount)
                .resolvedCount(resolvedCount)
                .totalCount(searchingCount + foundCount + resolvedCount)
                .build();
    }

    public List<LostItemListDto> getPendingItems() {
        return lostItemRepository.findByApprovedFalseOrderByIdDesc().stream()
                .map(this::toListDto)
                .collect(Collectors.toList());
    }

    public LostItem getItem(Long id) {
        return lostItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    public LostItemDetailDto getItemDetail(Long id) {
        LostItem item = getItem(id);
        LostItemType itemType = resolveItemType(item.getItemType());
        LostItemTypeStrategy itemTypeStrategy = lostItemTypeStrategyResolver.resolve(itemType);
        List<String> imagePaths = item.getImages().stream()
                .map(LostItemImage::getImagePath)
                .collect(Collectors.toList());
        List<CommentViewDto> comments = item.getComments().stream()
                .map(this::toCommentDto)
                .collect(Collectors.toList());

        return LostItemDetailDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .locationName(item.getLocationName())
                .contactInfo(item.getContactInfo())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .approved(item.isApproved())
                .resolved(item.getStatus() == LostItemStatus.RESOLVED)
                .categoryLabel(item.getCategory().getLabel())
                .categoryClass(toCategoryClass(item.getCategory()))
                .itemType(itemType)
                .itemTypeLabel(itemType.getLabel())
                .itemTypeClass(itemTypeStrategy.badgeClass())
                .itemTypeDescription(itemTypeStrategy.description())
                .statusLabel(item.getStatus().getLabel())
                .statusClass(toStatusClass(item.getStatus()))
                .createdAt(item.getCreatedAt())
                .imagePaths(imagePaths)
                .comments(comments)
                .build();
    }

    @Transactional
    public void addComment(Long itemId, String username, CommentCreateForm form) {
        LostItem item = getApprovedItem(itemId);
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("로그인한 사용자 정보를 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .author(member)
                .authorName(resolveDisplayName(member))
                .content(form.getContent().trim())
                .build();
        item.addComment(comment);
        commentRepository.save(comment);
    }

    @Transactional
    public void deleteCommentByAuthor(Long itemId, Long commentId, String username) {
        Comment comment = commentRepository.findByIdAndLostItemId(commentId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));

        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("로그인한 사용자 정보를 찾을 수 없습니다."));

        boolean isAdmin = member.getRole() == Role.ADMIN;
        boolean isAuthor = comment.getAuthor() != null && member.getId().equals(comment.getAuthor().getId());
        if (!isAdmin && !isAuthor) {
            throw new IllegalArgumentException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }

        commentRepository.delete(comment);
    }

    @Transactional
    public void deleteComment(Long itemId, Long commentId) {
        Comment comment = commentRepository.findByIdAndLostItemId(commentId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
        commentRepository.delete(comment);
    }

    @Transactional
    public void approveItem(Long itemId, String adminUsername) {
        Member admin = memberRepository.findByUsername(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("관리자 정보를 찾을 수 없습니다."));
        LostItem item = getItem(itemId);
        if (item.isApproved()) {
            return;
        }
        item.approve(admin);
    }

    @Transactional
    public void toggleResolved(Long itemId) {
        LostItem item = getItem(itemId);
        if (!item.isApproved()) {
            throw new IllegalArgumentException("승인된 게시글만 상태를 변경할 수 있습니다.");
        }

        if (item.getStatus() == LostItemStatus.RESOLVED) {
            item.restoreProgressStatus();
        } else {
            item.markResolved();
        }
    }

    @Transactional
    public void deleteItem(Long itemId) {
        LostItem item = getItem(itemId);
        List<String> storedFileNames = item.getImages().stream()
                .map(LostItemImage::getStoredFileName)
                .filter(Objects::nonNull)
                .toList();

        lostItemRepository.delete(item);
        storedFileNames.forEach(fileStoreService::deleteStoredFile);
    }

    private LostItem getApprovedItem(Long itemId) {
        LostItem item = getItem(itemId);
        if (!item.isApproved()) {
            throw new IllegalArgumentException("승인된 게시글만 댓글을 등록할 수 있습니다.");
        }
        return item;
    }

    private LostItemListDto toListDto(LostItem item) {
        LostItemType itemType = resolveItemType(item.getItemType());
        LostItemTypeStrategy itemTypeStrategy = lostItemTypeStrategyResolver.resolve(itemType);
        String firstImagePath = item.getImages().isEmpty() ? null : item.getImages().get(0).getImagePath();
        return LostItemListDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .locationName(item.getLocationName())
                .latitude(item.getLatitude())
                .longitude(item.getLongitude())
                .createdAt(item.getCreatedAt())
                .hasImage(firstImagePath != null)
                .firstImagePath(firstImagePath)
                .categoryLabel(item.getCategory().getLabel())
                .categoryClass(toCategoryClass(item.getCategory()))
                .itemType(itemType)
                .itemTypeLabel(itemType.getLabel())
                .itemTypeClass(itemTypeStrategy.badgeClass())
                .itemTypeMarkerColor(itemTypeStrategy.markerColorHex())
                .statusLabel(item.getStatus().getLabel())
                .statusClass(toStatusClass(item.getStatus()))
                .build();
    }

    private CommentViewDto toCommentDto(Comment comment) {
        return CommentViewDto.builder()
                .id(comment.getId())
                .authorName(comment.getAuthorName())
                .authorUsername(comment.getAuthor() != null ? comment.getAuthor().getUsername() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    private String toCategoryClass(LostItemCategory category) {
        return switch (category) {
            case REPORT -> "category-report";
            case SEARCH -> "category-search";
        };
    }

    private String toStatusClass(LostItemStatus status) {
        return switch (status) {
            case SEARCHING -> "status-searching";
            case FOUND -> "status-found";
            case RESOLVED -> "status-resolved";
        };
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveDisplayName(Member member) {
        String displayName = member.getDisplayName();
        return (displayName == null || displayName.isBlank()) ? member.getUsername() : displayName;
    }

    private LostItemType resolveItemType(LostItemType itemType) {
        return itemType != null ? itemType : LostItemType.OTHER;
    }
}
