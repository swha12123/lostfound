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
import com.example.lostfound.dto.LostItemUpdateForm;
import com.example.lostfound.strategy.itemtype.LostItemTypeStrategy;
import com.example.lostfound.strategy.itemtype.LostItemTypeStrategyResolver;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;

/**
 * 분실물 게시글, 댓글, 관리자 처리와 관련된 핵심 비즈니스 로직을 담당한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LostItemService {

    private static final int BOARD_PAGE_SIZE = 5;
    private static final Logger log = LoggerFactory.getLogger(LostItemService.class);
    private static final String IMAGE_UPLOAD_WARNING = "이미지 업로드에 실패했지만 텍스트 기반 게시글은 등록되었습니다.";

    private final LostItemRepository lostItemRepository;
    private final LostItemImageRepository lostItemImageRepository;
    private final CommentRepository commentRepository;
    private final FileStoreService fileStoreService;
    private final MemberRepository memberRepository;
    private final LostItemTypeStrategyResolver lostItemTypeStrategyResolver;

    /**
     * 게시글 작성 폼을 바탕으로 새 게시글을 저장하고, 이미지가 있으면 함께 업로드한다.
     *
     * @param form 작성 폼
     * @param imageFile 선택 이미지 파일
     * @return 생성된 게시글 ID와 경고 메시지
     */
    @Transactional
    public ItemCreateResult createAnonymousItem(LostItemCreateForm form, MultipartFile imageFile) {
        LostItem item = createItemFrom(form);
        lostItemRepository.save(item);

        // 이미지 업로드가 실패해도 텍스트 게시글은 남겨야 하므로 게시글을 먼저 저장한다.
        String warningMessage = storeImageIfPresent(item, imageFile);
        return new ItemCreateResult(item.getId(), warningMessage);
    }

    /**
     * 승인된 게시글 목록을 조회하고, 검색어가 있으면 전략 기반 필터를 적용한다.
     *
     * @param keyword 검색어
     * @return 게시글 목록 DTO
     */
    public List<LostItemListDto> getApprovedItems(String keyword) {
        String normalizedKeyword = normalizeKeyword(keyword);
        return filterItemsByKeyword(lostItemRepository.findByApprovedTrueOrderByIdDesc(), normalizedKeyword).stream()
                .map(this::toListDto)
                .toList();
    }

    /**
     * 게시판 종류별 승인 게시글을 페이지 단위로 조회한다.
     *
     * @param category 게시판 종류
     * @param itemType 물품 카테고리 필터
     * @param keyword 검색어
     * @param page 페이지 번호
     * @return 게시글 목록 페이지
     */
    public Page<LostItemListDto> getApprovedItemsByCategory(LostItemCategory category,
                                                            LostItemType itemType,
                                                            String keyword,
                                                            int page) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), BOARD_PAGE_SIZE);
        String normalizedKeyword = normalizeKeyword(keyword);
        if (normalizedKeyword == null) {
            return lostItemRepository.findApprovedPageByCategory(category, itemType, pageable)
                    .map(this::toListDto);
        }

        List<LostItem> filteredItems = filterItemsByKeyword(
                lostItemRepository.findApprovedListByCategory(category, itemType),
                normalizedKeyword
        );
        return toListDtoPage(filteredItems, pageable);
    }

    /**
     * 메인 화면 통계 카드에 사용할 상태별 게시글 수를 집계한다.
     *
     * @return 상태별 통계 DTO
     */
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

    /**
     * 관리자 승인 대기 게시글 목록을 조회한다.
     *
     * @return 승인되지 않은 게시글 목록
     */
    public List<LostItemListDto> getPendingItems() {
        return lostItemRepository.findByApprovedFalseOrderByIdDesc().stream()
                .map(this::toListDto)
                .toList();
    }

    /**
     * 게시글 ID로 엔티티를 조회한다.
     *
     * @param id 게시글 ID
     * @return 게시글 엔티티
     */
    public LostItem getItem(Long id) {
        return lostItemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시글을 찾을 수 없습니다."));
    }

    /**
     * 상세 화면에 필요한 게시글 정보를 DTO로 조합한다.
     *
     * @param id 게시글 ID
     * @return 상세 화면 DTO
     */
    public LostItemDetailDto getItemDetail(Long id) {
        return toDetailDto(getItem(id));
    }

    /**
     * 관리자 수정 화면에 표시할 기존 게시글 값을 폼으로 변환한다.
     *
     * @param id 게시글 ID
     * @return 관리자 수정 폼
     */
    public LostItemUpdateForm getAdminUpdateForm(Long id) {
        LostItem item = getItem(id);
        return LostItemUpdateForm.from(item, normalizePhoneNumber(item.getContactInfo()));
    }

    /**
     * 관리자 권한으로 게시글 내용을 수정한다.
     *
     * @param itemId 게시글 ID
     * @param form 수정 폼
     */
    @Transactional
    public void updateItemByAdmin(Long itemId, LostItemUpdateForm form) {
        LostItem item = getItem(itemId);
        item.updateDetails(
                form.getCategory(),
                form.getItemType(),
                form.getTitle(),
                form.getDescription(),
                form.getLocationName(),
                normalizePhoneNumber(form.getContactInfo()),
                form.getLatitude(),
                form.getLongitude()
        );
    }

    /**
     * 승인된 게시글에 댓글을 추가한다.
     *
     * @param itemId 게시글 ID
     * @param username 로그인 사용자 아이디
     * @param form 댓글 작성 폼
     */
    @Transactional
    public void addComment(Long itemId, String username, CommentCreateForm form) {
        LostItem item = getApprovedItem(itemId);
        Member member = findMemberByUsername(username, "로그인한 사용자 정보를 찾을 수 없습니다.");

        Comment comment = Comment.builder()
                .author(member)
                .authorName(resolveDisplayName(member))
                .content(form.getContent().trim())
                .build();
        item.addComment(comment);
        commentRepository.save(comment);
    }

    /**
     * 댓글 작성자 본인 또는 관리자인 경우 댓글을 삭제한다.
     *
     * @param itemId 게시글 ID
     * @param commentId 댓글 ID
     * @param username 로그인 사용자 아이디
     */
    @Transactional
    public void deleteCommentByAuthor(Long itemId, Long commentId, String username) {
        Comment comment = findComment(itemId, commentId);
        Member member = findMemberByUsername(username, "로그인한 사용자 정보를 찾을 수 없습니다.");

        // 컨트롤러를 우회하더라도 서비스 계층에서 권한 조건을 한 번 더 확인한다.
        validateCommentDeletionPermission(comment, member);
        commentRepository.delete(comment);
    }

    /**
     * 관리자 화면에서 댓글을 삭제한다.
     *
     * @param itemId 게시글 ID
     * @param commentId 댓글 ID
     */
    @Transactional
    public void deleteComment(Long itemId, Long commentId) {
        commentRepository.delete(findComment(itemId, commentId));
    }

    /**
     * 승인 대기 게시글을 승인 처리하고 승인한 관리자를 기록한다.
     *
     * @param itemId 게시글 ID
     * @param adminUsername 관리자 아이디
     */
    @Transactional
    public void approveItem(Long itemId, String adminUsername) {
        Member admin = findMemberByUsername(adminUsername, "관리자 정보를 찾을 수 없습니다.");
        LostItem item = getItem(itemId);
        if (item.isApproved()) {
            return;
        }
        item.approve(admin);
    }

    /**
     * 게시글 상태를 완료 또는 진행 중 상태로 전환한다.
     *
     * @param itemId 게시글 ID
     */
    @Transactional
    public void toggleResolved(Long itemId) {
        LostItem item = getItem(itemId);
        if (!item.isApproved()) {
            throw new IllegalArgumentException("승인된 게시글만 상태를 변경할 수 있습니다.");
        }

        // 완료 상태인 게시글은 원래 게시판 종류에 맞는 기본 진행 상태로 되돌린다.
        if (item.getStatus() == LostItemStatus.RESOLVED) {
            item.restoreProgressStatus();
        } else {
            item.markResolved();
        }
    }

    /**
     * 게시글과 연결된 저장 파일을 함께 정리한다.
     *
     * @param itemId 게시글 ID
     */
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

    /**
     * 작성 폼으로부터 게시글 엔티티를 생성한다.
     *
     * @param form 작성 폼
     * @return 게시글 엔티티
     */
    private LostItem createItemFrom(LostItemCreateForm form) {
        return LostItem.create(
                form.getCategory(),
                form.getItemType(),
                form.getTitle(),
                form.getDescription(),
                form.getLocationName(),
                normalizePhoneNumber(form.getContactInfo()),
                form.getLatitude(),
                form.getLongitude()
        );
    }

    /**
     * 이미지가 존재하면 업로드를 시도하고, 실패 시 경고 메시지를 반환한다.
     *
     * @param item 대상 게시글
     * @param imageFile 업로드 이미지 파일
     * @return 경고 메시지 또는 {@code null}
     */
    private String storeImageIfPresent(LostItem item, MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            return null;
        }

        try {
            saveItemImage(item, imageFile.getOriginalFilename(), fileStoreService.storeFile(imageFile));
            return null;
        } catch (ImageUploadFailedException e) {
            // 이미지 실패로 게시글 자체를 롤백하지 않도록 경고만 남기고 진행한다.
            log.warn("Image upload failed for item {}, saving text-only post instead.", item.getId(), e);
            return IMAGE_UPLOAD_WARNING;
        }
    }

    /**
     * 이미지 메타데이터를 게시글과 연결해 저장한다.
     *
     * @param item 대상 게시글
     * @param originalFileName 원본 파일명
     * @param storeResult 저장 파일명과 공개 경로
     */
    private void saveItemImage(LostItem item, String originalFileName, String[] storeResult) {
        LostItemImage image = LostItemImage.builder()
                .originalFileName(originalFileName)
                .storedFileName(storeResult[0])
                .imagePath(storeResult[1])
                .build();
        item.addImage(image);
        lostItemImageRepository.save(image);
    }

    /**
     * 공개 댓글 기능에서 사용할 게시글이 승인된 상태인지 확인한다.
     *
     * @param itemId 게시글 ID
     * @return 승인된 게시글
     */
    private LostItem getApprovedItem(Long itemId) {
        LostItem item = getItem(itemId);
        if (!item.isApproved()) {
            throw new IllegalArgumentException("승인된 게시글만 댓글을 등록할 수 있습니다.");
        }
        return item;
    }

    /**
     * 사용자 아이디로 회원을 조회한다.
     *
     * @param username 사용자 아이디
     * @param errorMessage 조회 실패 시 메시지
     * @return 회원 엔티티
     */
    private Member findMemberByUsername(String username, String errorMessage) {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException(errorMessage));
    }

    /**
     * 특정 게시글에 속한 댓글을 조회한다.
     *
     * @param itemId 게시글 ID
     * @param commentId 댓글 ID
     * @return 댓글 엔티티
     */
    private Comment findComment(Long itemId, Long commentId) {
        return commentRepository.findByIdAndLostItemId(commentId, itemId)
                .orElseThrow(() -> new IllegalArgumentException("댓글을 찾을 수 없습니다."));
    }

    /**
     * 댓글 삭제 권한이 있는지 확인한다.
     *
     * @param comment 대상 댓글
     * @param member 요청 사용자
     */
    private void validateCommentDeletionPermission(Comment comment, Member member) {
        boolean isAdmin = member.getRole() == Role.ADMIN;
        boolean isAuthor = comment.getAuthor() != null && member.getId().equals(comment.getAuthor().getId());
        if (!isAdmin && !isAuthor) {
            throw new IllegalArgumentException("본인이 작성한 댓글만 삭제할 수 있습니다.");
        }
    }

    /**
     * 검색어가 있을 때 전략 기반 필터를 적용한다.
     *
     * @param items 원본 게시글 목록
     * @param normalizedKeyword 정규화된 검색어
     * @return 필터링된 게시글 목록
     */
    private List<LostItem> filterItemsByKeyword(List<LostItem> items, String normalizedKeyword) {
        if (normalizedKeyword == null) {
            return items;
        }
        return items.stream()
                .filter(item -> matchesKeywordByStrategy(item, normalizedKeyword))
                .toList();
    }

    /**
     * 게시글 엔티티를 상세 화면 DTO로 변환한다.
     *
     * @param item 게시글 엔티티
     * @return 상세 화면 DTO
     */
    private LostItemDetailDto toDetailDto(LostItem item) {
        LostItemType itemType = resolveItemType(item.getItemType());
        LostItemTypeStrategy itemTypeStrategy = lostItemTypeStrategyResolver.resolve(itemType);

        return LostItemDetailDto.builder()
                .id(item.getId())
                .title(item.getTitle())
                .description(item.getDescription())
                .locationName(item.getLocationName())
                .contactInfo(normalizePhoneNumber(item.getContactInfo()))
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
                .imagePaths(item.getImages().stream().map(LostItemImage::getImagePath).toList())
                .comments(item.getComments().stream().map(this::toCommentDto).toList())
                .build();
    }

    /**
     * 게시글 엔티티를 목록 화면 DTO로 변환한다.
     *
     * @param item 게시글 엔티티
     * @return 목록 화면 DTO
     */
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

    /**
     * 댓글 엔티티를 화면 출력용 DTO로 변환한다.
     *
     * @param comment 댓글 엔티티
     * @return 댓글 화면 DTO
     */
    private CommentViewDto toCommentDto(Comment comment) {
        return CommentViewDto.builder()
                .id(comment.getId())
                .authorName(comment.getAuthorName())
                .authorUsername(comment.getAuthor() != null ? comment.getAuthor().getUsername() : null)
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .build();
    }

    /**
     * 게시판 종류를 CSS 클래스 이름으로 변환한다.
     *
     * @param category 게시판 종류
     * @return CSS 클래스명
     */
    private String toCategoryClass(LostItemCategory category) {
        return switch (category) {
            case REPORT -> "category-report";
            case SEARCH -> "category-search";
        };
    }

    /**
     * 상태 값을 CSS 클래스 이름으로 변환한다.
     *
     * @param status 게시글 상태
     * @return CSS 클래스명
     */
    private String toStatusClass(LostItemStatus status) {
        return switch (status) {
            case SEARCHING -> "status-searching";
            case FOUND -> "status-found";
            case RESOLVED -> "status-resolved";
        };
    }

    /**
     * 검색어를 trim 처리하고 비어 있으면 {@code null}로 간주한다.
     *
     * @param keyword 원본 검색어
     * @return 정규화된 검색어
     */
    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 댓글 표시용 이름을 결정한다.
     *
     * @param member 회원 엔티티
     * @return 표시 이름 또는 아이디
     */
    private String resolveDisplayName(Member member) {
        String displayName = member.getDisplayName();
        return (displayName == null || displayName.isBlank()) ? member.getUsername() : displayName;
    }

    /**
     * 물품 카테고리가 비어 있으면 OTHER로 보정한다.
     *
     * @param itemType 원본 물품 카테고리
     * @return null 이 아닌 물품 카테고리
     */
    private LostItemType resolveItemType(LostItemType itemType) {
        return itemType != null ? itemType : LostItemType.OTHER;
    }

    /**
     * 전략 패턴을 이용해 검색어 일치 여부를 판단한다.
     *
     * @param item 대상 게시글
     * @param keyword 정규화된 검색어
     * @return 일치 여부
     */
    private boolean matchesKeywordByStrategy(LostItem item, String keyword) {
        LostItemType itemType = resolveItemType(item.getItemType());
        LostItemTypeStrategy strategy = lostItemTypeStrategyResolver.resolve(itemType);
        return strategy.matchesKeyword(item, keyword);
    }

    /**
     * 메모리에서 필터링된 게시글 목록을 페이지 객체로 변환한다.
     *
     * @param items 필터링된 게시글 목록
     * @param pageable 페이지 요청 정보
     * @return 목록 페이지
     */
    private Page<LostItemListDto> toListDtoPage(List<LostItem> items, Pageable pageable) {
        int start = Math.toIntExact(pageable.getOffset());
        if (start >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }

        int end = Math.min(start + pageable.getPageSize(), items.size());
        List<LostItemListDto> content = items.subList(start, end).stream()
                .map(this::toListDto)
                .toList();
        return new PageImpl<>(content, pageable, items.size());
    }

    /**
     * 연락처를 010-0000-0000 형식으로 정규화한다.
     *
     * @param contactInfo 원본 연락처
     * @return 정규화된 연락처 또는 원본 문자열
     */
    private String normalizePhoneNumber(String contactInfo) {
        if (contactInfo == null) {
            return null;
        }

        String trimmed = contactInfo.trim();
        String digitsOnly = trimmed.replaceAll("\\D", "");
        if (digitsOnly.matches("010\\d{8}")) {
            return digitsOnly.substring(0, 3) + "-"
                    + digitsOnly.substring(3, 7) + "-"
                    + digitsOnly.substring(7);
        }
        return trimmed;
    }
}
