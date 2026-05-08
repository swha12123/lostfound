package com.example.lostfound.controller;

import com.example.lostfound.dto.CommentCreateForm;
import com.example.lostfound.dto.LostItemDetailDto;
import com.example.lostfound.dto.LostItemListDto;
import com.example.lostfound.service.LostItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/admin")
public class AdminController {

    private final LostItemService lostItemService;

    @GetMapping("/pending")
    public String pendingList(Model model) {
        List<LostItemListDto> items = lostItemService.getPendingItems();
        model.addAttribute("items", items);
        return "admin/pending";
    }

    @PostMapping("/approve-ajax/{id}")
    @ResponseBody
    public ResponseEntity<String> approveItemAjax(@PathVariable Long id,
                                                  @AuthenticationPrincipal UserDetails userDetails) {
        lostItemService.approveItem(id, userDetails.getUsername());
        return ResponseEntity.ok("승인되었습니다.");
    }

    @GetMapping("/items/{id}")
    public String adminDetail(@PathVariable Long id, Model model) {
        LostItemDetailDto dto = lostItemService.getItemDetail(id);
        model.addAttribute("item", dto);
        model.addAttribute("commentForm", new CommentCreateForm());
        return "items/detail";
    }

    @PostMapping("/items/{id}/toggle-resolved")
    public String toggleResolved(@PathVariable Long id) {
        lostItemService.toggleResolved(id);
        return "redirect:/items/" + id;
    }

    @PostMapping("/items/{itemId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long itemId,
                                @PathVariable Long commentId) {
        lostItemService.deleteComment(itemId, commentId);
        return "redirect:/admin/items/" + itemId;
    }

    @PostMapping("/items/{id}/delete")
    public String deleteItem(@PathVariable Long id,
                             @RequestParam(defaultValue = "pending") String source) {
        lostItemService.deleteItem(id);
        if ("detail".equals(source)) {
            return "redirect:/items/list";
        }
        return "redirect:/admin/pending";
    }
}