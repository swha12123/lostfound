package com.example.lostfound.controller;

import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.domain.enums.LostItemType;
import com.example.lostfound.dto.CommentCreateForm;
import com.example.lostfound.dto.LostItemDetailDto;
import com.example.lostfound.dto.LostItemListDto;
import com.example.lostfound.dto.LostItemUpdateForm;
import com.example.lostfound.service.LostItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

/**
 * 관리자 승인, 삭제, 상태 변경과 같은 운영 기능 요청을 처리한다.
 */
@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final LostItemService lostItemService;

    /**
     * 승인 대기 목록 화면을 렌더링한다.
     *
     * @param model 뷰 모델
     * @return 승인 대기 목록 템플릿
     */
    @GetMapping("/pending")
    public String pendingList(Model model) {
        List<LostItemListDto> items = lostItemService.getPendingItems();
        model.addAttribute("items", items);
        return "admin/pending";
    }

    /**
     * 비동기 요청으로 게시글을 승인 처리한다.
     *
     * @param id 게시글 ID
     * @param userDetails 로그인 관리자
     * @return 승인 결과 메시지
     */
    @PostMapping("/approve-ajax/{id}")
    @ResponseBody
    public ResponseEntity<String> approveItemAjax(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        try {
            lostItemService.approveItem(id, userDetails.getUsername());
            return ResponseEntity.ok("승인 처리되었습니다");
        } catch (IllegalArgumentException e) {
            // 응답은 단순 문자열로 유지하고, 실패 원인은 로그로 남겨 운영 중 추적 가능하게 한다.
            log.warn("Failed to approve item {}", id, e);
            return ResponseEntity.ok("이미 처리된 요청입니다");
        }
    }

    /**
     * 관리자 상세 화면을 렌더링한다.
     *
     * @param id 게시글 ID
     * @param model 뷰 모델
     * @return 상세 화면 템플릿
     */
    @GetMapping("/items/{id}")
    public String adminDetail(@PathVariable Long id, Model model) {
        LostItemDetailDto dto = lostItemService.getItemDetail(id);
        model.addAttribute("item", dto);
        model.addAttribute("commentForm", new CommentCreateForm());
        model.addAttribute("adminView", true);
        return "items/detail";
    }

    /**
     * 관리자 게시글 수정 화면을 렌더링한다.
     *
     * @param id 게시글 ID
     * @param model 뷰 모델
     * @return 수정 화면 템플릿
     */
    @GetMapping("/items/{id}/edit")
    public String editItemForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", lostItemService.getAdminUpdateForm(id));
        populateEditModel(id, model);
        return "items/edit";
    }

    /**
     * 관리자 권한으로 게시글 수정 요청을 처리한다.
     *
     * @param id 게시글 ID
     * @param form 수정 폼
     * @param bindingResult 검증 결과
     * @param model 뷰 모델
     * @param redirectAttributes 리다이렉트 속성
     * @return 다음 화면 또는 리다이렉트 경로
     */
    @PostMapping("/items/{id}/edit")
    public String updateItem(@PathVariable Long id,
                             @Valid @ModelAttribute("form") LostItemUpdateForm form,
                             BindingResult bindingResult,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            populateEditModel(id, model);
            return "items/edit";
        }

        try {
            lostItemService.updateItemByAdmin(id, form);
            redirectAttributes.addFlashAttribute("adminMessage", "게시글이 수정되었습니다.");
            return "redirect:/admin/items/" + id;
        } catch (IllegalArgumentException e) {
            log.warn("Failed to update item {}", id, e);
            model.addAttribute("itemError", e.getMessage());
            populateEditModel(id, model);
            return "items/edit";
        }
    }

    /**
     * 게시글 상태를 완료 또는 진행 중 상태로 전환한다.
     *
     * @param id 게시글 ID
     * @return 공개 상세 화면 리다이렉트 경로
     */
    @PostMapping("/items/{id}/toggle-resolved")
    public String toggleResolved(@PathVariable Long id) {
        try {
            lostItemService.toggleResolved(id);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to toggle resolved state for item {}", id, e);
        }
        return "redirect:/items/" + id;
    }

    /**
     * 관리자 화면에서 댓글을 삭제한다.
     *
     * @param itemId 게시글 ID
     * @param commentId 댓글 ID
     * @return 관리자 상세 화면 리다이렉트 경로
     */
    @PostMapping("/items/{itemId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long itemId,
                                @PathVariable Long commentId) {
        try {
            lostItemService.deleteComment(itemId, commentId);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete comment {} for item {}", commentId, itemId, e);
        }
        return "redirect:/admin/items/" + itemId;
    }

    /**
     * 게시글을 삭제하고, 요청이 들어온 화면에 맞춰 리다이렉트한다.
     *
     * @param id 게시글 ID
     * @param source 요청 출처
     * @return 리다이렉트 경로
     */
    @PostMapping("/items/{id}/delete")
    public String deleteItem(@PathVariable Long id,
                             @RequestParam(defaultValue = "pending") String source) {
        try {
            lostItemService.deleteItem(id);
        } catch (IllegalArgumentException e) {
            log.warn("Failed to delete item {}", id, e);
        }
        if ("detail".equals(source)) {
            return "redirect:/items/list";
        }
        return "redirect:/admin/pending";
    }

    private void populateEditModel(Long id, Model model) {
        model.addAttribute("itemId", id);
        model.addAttribute("item", lostItemService.getItemDetail(id));
        model.addAttribute("categories", LostItemCategory.values());
        model.addAttribute("itemTypes", LostItemType.values());
    }
}
