package com.example.lostfound.controller;

import com.example.lostfound.domain.enums.LostItemCategory;
import com.example.lostfound.dto.CommentCreateForm;
import com.example.lostfound.dto.LostItemCreateForm;
import com.example.lostfound.dto.LostItemDetailDto;
import com.example.lostfound.dto.LostItemListDto;
import com.example.lostfound.service.LostItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private final LostItemService lostItemService;

    @GetMapping("/")
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(defaultValue = "0") int reportPage,
                        @RequestParam(defaultValue = "0") int searchPage,
                        Model model) {
        populateListModel(q, reportPage, searchPage, model);
        return "items/list";
    }

    @GetMapping("/items/list")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(defaultValue = "0") int reportPage,
                       @RequestParam(defaultValue = "0") int searchPage,
                       Model model) {
        populateListModel(q, reportPage, searchPage, model);
        return "items/list";
    }

    @GetMapping("/items/{id}")
    public String detail(@PathVariable Long id, Model model) {
        populatePublicDetailModel(id, model, new CommentCreateForm());
        return "items/detail";
    }

    @GetMapping("/items/new")
    public String createForm(Model model) {
        model.addAttribute("form", new LostItemCreateForm());
        model.addAttribute("categories", LostItemCategory.values());
        return "items/create";
    }

    @PostMapping("/items")
    public String create(@Valid @ModelAttribute("form") LostItemCreateForm form,
                         BindingResult bindingResult,
                         @RequestParam("imageFile") MultipartFile imageFile,
                         Model model) {
        model.addAttribute("categories", LostItemCategory.values());

        if (bindingResult.hasErrors()) {
            return "items/create";
        }

        try {
            lostItemService.createAnonymousItem(form, imageFile);
        } catch (IllegalArgumentException e) {
            model.addAttribute("fileError", e.getMessage());
            return "items/create";
        }
        return "redirect:/items/list";
    }

    @PostMapping("/items/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @Valid @ModelAttribute("commentForm") CommentCreateForm commentForm,
                             BindingResult bindingResult,
                             Model model) {
        if (bindingResult.hasErrors()) {
            populatePublicDetailModel(id, model, commentForm);
            return "items/detail";
        }

        try {
            lostItemService.addComment(id, commentForm);
            return "redirect:/items/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("commentError", e.getMessage());
            populatePublicDetailModel(id, model, commentForm);
            return "items/detail";
        }
    }

    @PostMapping("/items/{itemId}/comments/{commentId}/delete")
    public String deleteCommentByPassword(@PathVariable Long itemId,
                                          @PathVariable Long commentId,
                                          @RequestParam(defaultValue = "") String commentPassword,
                                          Model model) {
        try {
            lostItemService.deleteCommentByPassword(itemId, commentId, commentPassword);
            return "redirect:/items/" + itemId;
        } catch (IllegalArgumentException e) {
            model.addAttribute("commentDeleteError", e.getMessage());
            populatePublicDetailModel(itemId, model, new CommentCreateForm());
            return "items/detail";
        }
    }

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    private void populateListModel(String keyword, int reportPage, int searchPage, Model model) {
        Page<LostItemListDto> reportPageData = lostItemService.getApprovedItemsByCategory(LostItemCategory.REPORT, keyword, reportPage);
        Page<LostItemListDto> searchPageData = lostItemService.getApprovedItemsByCategory(LostItemCategory.SEARCH, keyword, searchPage);

        model.addAttribute("reportPageData", reportPageData);
        model.addAttribute("searchPageData", searchPageData);
        model.addAttribute("reportItems", reportPageData.getContent());
        model.addAttribute("searchItems", searchPageData.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("stats", lostItemService.getApprovedStatistics());
    }

    private void populatePublicDetailModel(Long id, Model model, CommentCreateForm commentForm) {
        LostItemDetailDto dto = lostItemService.getItemDetail(id);
        if (!dto.isApproved()) {
            throw new IllegalArgumentException("승인되지 않은 게시글입니다.");
        }
        model.addAttribute("item", dto);
        model.addAttribute("commentForm", commentForm);
    }
}