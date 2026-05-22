package com.example.lostfound.service;

import com.example.lostfound.domain.entity.Comment;
import com.example.lostfound.domain.entity.LostItem;
import com.example.lostfound.domain.entity.Member;
import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemType;
import com.example.lostfound.domain.enums.Role;
import com.example.lostfound.domain.repository.CommentRepository;
import com.example.lostfound.domain.repository.LostItemImageRepository;
import com.example.lostfound.domain.repository.LostItemRepository;
import com.example.lostfound.domain.repository.MemberRepository;
import com.example.lostfound.dto.LostItemCreateForm;
import com.example.lostfound.dto.LostItemDetailDto;
import com.example.lostfound.dto.LostItemListDto;
import com.example.lostfound.strategy.itemtype.BagItemTypeStrategy;
import com.example.lostfound.strategy.itemtype.ClothingItemTypeStrategy;
import com.example.lostfound.strategy.itemtype.ElectronicsItemTypeStrategy;
import com.example.lostfound.strategy.itemtype.IdCardItemTypeStrategy;
import com.example.lostfound.strategy.itemtype.KeysItemTypeStrategy;
import com.example.lostfound.strategy.itemtype.LostItemTypeStrategyResolver;
import com.example.lostfound.strategy.itemtype.OtherItemTypeStrategy;
import com.example.lostfound.strategy.itemtype.WalletItemTypeStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LostItemServiceTest {

    @Mock
    private LostItemRepository lostItemRepository;

    @Mock
    private LostItemImageRepository lostItemImageRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private FileStoreService fileStoreService;

    @Mock
    private MemberRepository memberRepository;

    private LostItemService lostItemService;

    @BeforeEach
    void setUp() {
        LostItemTypeStrategyResolver resolver = new LostItemTypeStrategyResolver(List.of(
                new WalletItemTypeStrategy(),
                new ElectronicsItemTypeStrategy(),
                new IdCardItemTypeStrategy(),
                new BagItemTypeStrategy(),
                new ClothingItemTypeStrategy(),
                new KeysItemTypeStrategy(),
                new OtherItemTypeStrategy()
        ));
        lostItemService = new LostItemService(
                lostItemRepository,
                lostItemImageRepository,
                commentRepository,
                fileStoreService,
                memberRepository,
                resolver
        );
    }

    @Test
    void createAnonymousItemStoresCoordinatesAndItemType() {
        LostItemCreateForm form = new LostItemCreateForm();
        form.setCategory(LostItemCategory.REPORT);
        form.setItemType(LostItemType.WALLET);
        form.setTitle("지갑을 주웠어요");
        form.setDescription("도서관 앞에서 발견");
        form.setLocationName("중앙도서관 앞");
        form.setContactInfo("010-1234-5678");
        form.setLatitude(37.5052);
        form.setLongitude(126.9571);

        when(lostItemRepository.save(any(LostItem.class))).thenAnswer(invocation -> {
            LostItem item = invocation.getArgument(0);
            item.setId(11L);
            return item;
        });

        ItemCreateResult savedResult = lostItemService.createAnonymousItem(form, new MockMultipartFile("imageFile", new byte[0]));

        assertEquals(11L, savedResult.itemId());
        assertNull(savedResult.warningMessage());
        verify(lostItemRepository).save(argThat(matchesForm(form)));
    }

    @Test
    void createAnonymousItemKeepsTextPostWhenImageUploadFails() {
        LostItemCreateForm form = new LostItemCreateForm();
        form.setCategory(LostItemCategory.REPORT);
        form.setItemType(LostItemType.WALLET);
        form.setTitle("지갑을 주웠어요");
        form.setDescription("도서관 앞에서 발견");
        form.setLocationName("중앙도서관 앞");
        form.setContactInfo("010-1234-5678");
        form.setLatitude(37.5052);
        form.setLongitude(126.9571);

        MockMultipartFile imageFile = new MockMultipartFile("imageFile", "wallet.png", "image/png", new byte[]{1, 2, 3});

        when(lostItemRepository.save(any(LostItem.class))).thenAnswer(invocation -> {
            LostItem item = invocation.getArgument(0);
            item.setId(12L);
            return item;
        });
        when(fileStoreService.storeFile(imageFile))
                .thenThrow(new ImageUploadFailedException("이미지 업로드 실패", new RuntimeException("S3 error")));

        ItemCreateResult savedResult = lostItemService.createAnonymousItem(form, imageFile);

        assertEquals(12L, savedResult.itemId());
        assertEquals("이미지 업로드에 실패했지만 텍스트 기반 게시글은 등록되었습니다.", savedResult.warningMessage());
        verify(lostItemRepository).save(any(LostItem.class));
        verify(lostItemImageRepository, never()).save(any());
    }

    @Test
    void deleteCommentByAuthorAllowsOwner() {
        Member author = Member.builder()
                .username("writer")
                .password("encoded")
                .displayName("작성자")
                .role(Role.USER)
                .build();
        author.setId(1L);

        Comment comment = Comment.builder()
                .author(author)
                .authorName("작성자")
                .content("제보 댓글")
                .build();
        comment.setId(10L);

        when(commentRepository.findByIdAndLostItemId(10L, 3L)).thenReturn(Optional.of(comment));
        when(memberRepository.findByUsername("writer")).thenReturn(Optional.of(author));

        lostItemService.deleteCommentByAuthor(3L, 10L, "writer");

        verify(commentRepository).delete(comment);
    }

    @Test
    void deleteCommentByAuthorRejectsDifferentUser() {
        Member author = Member.builder()
                .username("writer")
                .password("encoded")
                .displayName("작성자")
                .role(Role.USER)
                .build();
        author.setId(1L);

        Member other = Member.builder()
                .username("other")
                .password("encoded")
                .displayName("다른사람")
                .role(Role.USER)
                .build();
        other.setId(2L);

        Comment comment = Comment.builder()
                .author(author)
                .authorName("작성자")
                .content("제보 댓글")
                .build();
        comment.setId(10L);

        when(commentRepository.findByIdAndLostItemId(10L, 3L)).thenReturn(Optional.of(comment));
        when(memberRepository.findByUsername("other")).thenReturn(Optional.of(other));

        assertThrows(IllegalArgumentException.class,
                () -> lostItemService.deleteCommentByAuthor(3L, 10L, "other"));

        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    void getApprovedItemsByCategoryUsesSearchStrategyKeywords() {
        LostItem electronics = approvedItem(21L, LostItemCategory.REPORT, LostItemType.ELECTRONICS,
                "검은색 케이스 보관 중", "기기 본체가 들어 있습니다.", "학생회관 1층");
        LostItem bag = approvedItem(22L, LostItemCategory.REPORT, LostItemType.BAG,
                "가죽 파우치 보관 중", "작은 소지품이 들어 있습니다.", "도서관 앞");

        when(lostItemRepository.findApprovedListByCategory(LostItemCategory.REPORT, null))
                .thenReturn(List.of(electronics, bag));

        Page<LostItemListDto> result = lostItemService.getApprovedItemsByCategory(
                LostItemCategory.REPORT,
                null,
                "전자기기",
                0
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(21L, result.getContent().get(0).getId());
        assertEquals(LostItemType.ELECTRONICS, result.getContent().get(0).getItemType());
    }

    @Test
    void getItemDetailFormatsPhoneNumber() {
        LostItem item = approvedItem(31L, LostItemCategory.REPORT, LostItemType.WALLET,
                "지갑 보관 중", "학생회관 앞에서 발견", "학생회관 앞");
        item.setContactInfo("01012345678");

        when(lostItemRepository.findById(31L)).thenReturn(Optional.of(item));

        LostItemDetailDto detail = lostItemService.getItemDetail(31L);

        assertEquals("010-1234-5678", detail.getContactInfo());
    }

    private ArgumentMatcher<LostItem> matchesForm(LostItemCreateForm form) {
        return item -> item.getCategory() == form.getCategory()
                && item.getItemType() == form.getItemType()
                && item.getTitle().equals(form.getTitle())
                && item.getLocationName().equals(form.getLocationName())
                && item.getContactInfo().equals(form.getContactInfo())
                && item.getLatitude().equals(form.getLatitude())
                && item.getLongitude().equals(form.getLongitude());
    }

    private LostItem approvedItem(Long id,
                                  LostItemCategory category,
                                  LostItemType itemType,
                                  String title,
                                  String description,
                                  String locationName) {
        LostItem item = LostItem.create(
                category,
                itemType,
                title,
                description,
                locationName,
                "010-0000-0000",
                37.0,
                127.0
        );
        item.setId(id);
        item.setApproved(true);
        item.setCreatedAt(LocalDateTime.now());
        return item;
    }
}
