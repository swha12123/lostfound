package com.example.lostfound.controller;

import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemType;
import com.example.lostfound.dto.CommentCreateForm;
import com.example.lostfound.dto.LostItemCreateForm;
import com.example.lostfound.dto.LostItemDetailDto;
import com.example.lostfound.dto.LostItemListDto;
import com.example.lostfound.service.LostItemService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * 공개 목록, 상세, 등록, 댓글 기능과 관련된 요청을 처리한다.
 */
@Controller
@RequiredArgsConstructor
public class ItemController {

    private static final String CREATE_ITEM_TOKEN_SESSION_KEY = "createItemToken";

    private final LostItemService lostItemService;

    /**
     * 메인 화면을 렌더링한다.
     *
     * @param q 검색어
     * @param itemType 물품 카테고리 필터
     * @param reportPage 분실물 제보 게시판 페이지
     * @param searchPage 분실물 찾기 게시판 페이지
     * @param model 뷰 모델
     * @return 목록 화면 템플릿
     */
    @GetMapping("/")
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) LostItemType itemType,
                        @RequestParam(defaultValue = "0") int reportPage,
                        @RequestParam(defaultValue = "0") int searchPage,
                        Model model) {
        populateListModel(q, itemType, reportPage, searchPage, model);
        return "items/list";
    }

    /**
     * 목록 화면을 렌더링한다.
     *
     * @param q 검색어
     * @param itemType 물품 카테고리 필터
     * @param reportPage 분실물 제보 게시판 페이지
     * @param searchPage 분실물 찾기 게시판 페이지
     * @param model 뷰 모델
     * @return 목록 화면 템플릿
     */
    @GetMapping("/items/list")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) LostItemType itemType,
                       @RequestParam(defaultValue = "0") int reportPage,
                       @RequestParam(defaultValue = "0") int searchPage,
                       Model model) {
        populateListModel(q, itemType, reportPage, searchPage, model);
        return "items/list";
    }

    /**
     * 게시글 상세 화면을 렌더링한다.
     *
     * @param id 게시글 ID
     * @param model 뷰 모델
     * @return 상세 화면 템플릿
     */
    @GetMapping("/items/{id}")
    public String detail(@PathVariable Long id, Model model) {
        populatePublicDetailModel(id, model, new CommentCreateForm());
        return "items/detail";
    }

    /**
     * 게시글 등록 화면을 렌더링한다.
     *
     * @param model 뷰 모델
     * @param session 현재 세션
     * @return 등록 화면 템플릿
     */
    @GetMapping("/items/new")
    public String createForm(Model model, HttpSession session) {
        model.addAttribute("form", new LostItemCreateForm());
        populateCreateModel(model, session);
        return "items/create";
    }

    /**
     * 게시글 등록 요청을 처리한다.
     *
     * @param form 작성 폼
     * @param bindingResult 검증 결과
     * @param imageFile 선택 이미지 파일
     * @param createToken 중복 제출 방지 토큰
     * @param model 뷰 모델
     * @param session 현재 세션
     * @param redirectAttributes 리다이렉트 속성
     * @return 다음 화면 또는 리다이렉트 경로
     */
    @PostMapping("/items")
    public String create(@Valid @ModelAttribute("form") LostItemCreateForm form,
                         BindingResult bindingResult,
                         @RequestParam("imageFile") MultipartFile imageFile,
                         @RequestParam("createToken") String createToken,
                         Model model,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        populateCreateOptions(model);

        if (bindingResult.hasErrors()) {
            keepOrRefreshCreateToken(model, session, createToken);
            return "items/create";
        }

        // 새로고침이나 더블 클릭으로 게시글이 중복 생성되지 않도록 토큰을 한 번만 소비한다.
        if (!consumeCreateToken(session, createToken)) {
            return "redirect:/items/list";
        }

        try {
            var result = lostItemService.createAnonymousItem(form, imageFile);
            if (result.warningMessage() != null) {
                redirectAttributes.addFlashAttribute("postCreateWarning", result.warningMessage());
            }
            return "redirect:/items/list";
        } catch (IllegalArgumentException e) {
            model.addAttribute("fileError", e.getMessage());
            issueCreateToken(model, session);
            return "items/create";
        }
    }

    /**
     * 댓글 등록 요청을 처리한다.
     *
     * @param id 게시글 ID
     * @param commentForm 댓글 폼
     * @param bindingResult 검증 결과
     * @param userDetails 로그인 사용자
     * @param model 뷰 모델
     * @return 다음 화면 또는 리다이렉트 경로
     */
    @PostMapping("/items/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @Valid @ModelAttribute("commentForm") CommentCreateForm commentForm,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        if (bindingResult.hasErrors()) {
            return renderDetailWithCommentState(id, commentForm, model, null, null);
        }

        try {
            lostItemService.addComment(id, userDetails.getUsername(), commentForm);
            return "redirect:/items/" + id;
        } catch (IllegalArgumentException e) {
            return renderDetailWithCommentState(id, commentForm, model, "commentError", e.getMessage());
        }
    }

    /**
     * 댓글 삭제 요청을 처리한다.
     *
     * @param itemId 게시글 ID
     * @param commentId 댓글 ID
     * @param userDetails 로그인 사용자
     * @param model 뷰 모델
     * @return 다음 화면 또는 리다이렉트 경로
     */
    @PostMapping("/items/{itemId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long itemId,
                                @PathVariable Long commentId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        try {
            lostItemService.deleteCommentByAuthor(itemId, commentId, userDetails.getUsername());
            return "redirect:/items/" + itemId;
        } catch (IllegalArgumentException e) {
            return renderDetailWithCommentState(
                    itemId,
                    new CommentCreateForm(),
                    model,
                    "commentDeleteError",
                    e.getMessage()
            );
        }
    }

    /**
     * 댓글 처리 중 오류가 발생했을 때 상세 화면 모델을 다시 구성한다.
     *
     * @param id 게시글 ID
     * @param commentForm 현재 댓글 폼 상태
     * @param model 뷰 모델
     * @param errorAttributeName 오류 속성명
     * @param errorMessage 오류 메시지
     * @return 상세 화면 템플릿
     */
    private String renderDetailWithCommentState(Long id,
                                                CommentCreateForm commentForm,
                                                Model model,
                                                String errorAttributeName,
                                                String errorMessage) {
        if (errorAttributeName != null) {
            model.addAttribute(errorAttributeName, errorMessage);
        }
        populatePublicDetailModel(id, model, commentForm);
        return "items/detail";
    }

    /**
     * 등록 화면 공통 속성과 토큰을 함께 설정한다.
     *
     * @param model 뷰 모델
     * @param session 현재 세션
     */
    private void populateCreateModel(Model model, HttpSession session) {
        populateCreateOptions(model);
        issueCreateToken(model, session);
    }

    /**
     * 등록 화면에서 사용하는 enum 목록을 모델에 추가한다.
     *
     * @param model 뷰 모델
     */
    private void populateCreateOptions(Model model) {
        model.addAttribute("categories", LostItemCategory.values());
        model.addAttribute("itemTypes", LostItemType.values());
    }

    /**
     * 등록 토큰을 새로 발급해 세션과 모델에 저장한다.
     *
     * @param model 뷰 모델
     * @param session 현재 세션
     */
    private void issueCreateToken(Model model, HttpSession session) {
        String token = UUID.randomUUID().toString();
        synchronized (WebUtils.getSessionMutex(session)) {
            session.setAttribute(CREATE_ITEM_TOKEN_SESSION_KEY, token);
        }
        model.addAttribute("createToken", token);
    }

    /**
     * 검증 오류가 났을 때 기존 토큰을 유지하거나 새 토큰을 발급한다.
     *
     * @param model 뷰 모델
     * @param session 현재 세션
     * @param createToken 제출된 토큰
     */
    private void keepOrRefreshCreateToken(Model model, HttpSession session, String createToken) {
        if (hasCurrentCreateToken(session, createToken)) {
            model.addAttribute("createToken", createToken);
            return;
        }
        issueCreateToken(model, session);
    }

    /**
     * 제출된 토큰이 현재 세션 토큰과 일치하는지 확인한다.
     *
     * @param session 현재 세션
     * @param createToken 제출된 토큰
     * @return 토큰 일치 여부
     */
    private boolean hasCurrentCreateToken(HttpSession session, String createToken) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Object storedToken = session.getAttribute(CREATE_ITEM_TOKEN_SESSION_KEY);
            return storedToken instanceof String token && token.equals(createToken);
        }
    }

    /**
     * 등록 토큰을 한 번만 사용할 수 있도록 소비한다.
     *
     * @param session 현재 세션
     * @param createToken 제출된 토큰
     * @return 소비 성공 여부
     */
    private boolean consumeCreateToken(HttpSession session, String createToken) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Object storedToken = session.getAttribute(CREATE_ITEM_TOKEN_SESSION_KEY);
            if (!(storedToken instanceof String token) || !token.equals(createToken)) {
                return false;
            }
            session.removeAttribute(CREATE_ITEM_TOKEN_SESSION_KEY);
            return true;
        }
    }

    /**
     * 목록 화면에 필요한 게시판, 통계, 지도 데이터를 모델에 채운다.
     *
     * @param keyword 검색어
     * @param itemType 물품 카테고리 필터
     * @param reportPage 분실물 제보 게시판 페이지
     * @param searchPage 분실물 찾기 게시판 페이지
     * @param model 뷰 모델
     */
    private void populateListModel(String keyword,
                                   LostItemType itemType,
                                   int reportPage,
                                   int searchPage,
                                   Model model) {
        Page<LostItemListDto> reportPageData = lostItemService.getApprovedItemsByCategory(
                LostItemCategory.REPORT, itemType, keyword, reportPage
        );
        Page<LostItemListDto> searchPageData = lostItemService.getApprovedItemsByCategory(
                LostItemCategory.SEARCH, itemType, keyword, searchPage
        );

        model.addAttribute("reportPageData", reportPageData);
        model.addAttribute("searchPageData", searchPageData);
        model.addAttribute("reportItems", reportPageData.getContent());
        model.addAttribute("searchItems", searchPageData.getContent());
        model.addAttribute("mapItems", extractMapItems(reportPageData, searchPageData));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedItemType", itemType);
        model.addAttribute("itemTypes", LostItemType.values());
        model.addAttribute("stats", lostItemService.getApprovedStatistics());
    }

    /**
     * 좌표가 있는 게시글만 추려 지도 마커 데이터로 사용한다.
     *
     * @param reportPageData 분실물 제보 게시판 데이터
     * @param searchPageData 분실물 찾기 게시판 데이터
     * @return 지도 표시용 게시글 목록
     */
    private List<LostItemListDto> extractMapItems(Page<LostItemListDto> reportPageData,
                                                  Page<LostItemListDto> searchPageData) {
        // 지도에는 현재 화면에 보이는 게시글 중 좌표가 있는 항목만 올린다.
        return Stream.concat(reportPageData.getContent().stream(), searchPageData.getContent().stream())
                .filter(item -> item.getLatitude() != null && item.getLongitude() != null)
                .toList();
    }

    /**
     * 공개 상세 화면 모델을 구성하고 승인되지 않은 게시글은 차단한다.
     *
     * @param id 게시글 ID
     * @param model 뷰 모델
     * @param commentForm 화면에 유지할 댓글 폼
     */
    private void populatePublicDetailModel(Long id, Model model, CommentCreateForm commentForm) {
        LostItemDetailDto dto = lostItemService.getItemDetail(id);
        if (!dto.isApproved()) {
            throw new IllegalArgumentException("승인되지 않은 게시글입니다.");
        }
        model.addAttribute("item", dto);
        model.addAttribute("commentForm", commentForm);
    }
}
