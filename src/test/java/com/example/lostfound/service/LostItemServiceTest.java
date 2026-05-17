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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

        Long savedId = lostItemService.createAnonymousItem(form, new MockMultipartFile("imageFile", new byte[0]));

        assertEquals(11L, savedId);
        verify(lostItemRepository).save(argThat(matchesForm(form)));
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

    private ArgumentMatcher<LostItem> matchesForm(LostItemCreateForm form) {
        return item -> item.getCategory() == form.getCategory()
                && item.getItemType() == form.getItemType()
                && item.getTitle().equals(form.getTitle())
                && item.getLocationName().equals(form.getLocationName())
                && item.getContactInfo().equals(form.getContactInfo())
                && item.getLatitude().equals(form.getLatitude())
                && item.getLongitude().equals(form.getLongitude());
    }
}
