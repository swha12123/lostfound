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
import org.springframework.web.util.WebUtils;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@Controller
@RequiredArgsConstructor
public class ItemController {

    private static final String CREATE_ITEM_TOKEN_SESSION_KEY = "createItemToken";

    private final LostItemService lostItemService;

    @GetMapping("/")
    public String index(@RequestParam(required = false) String q,
                        @RequestParam(required = false) LostItemType itemType,
                        @RequestParam(defaultValue = "0") int reportPage,
                        @RequestParam(defaultValue = "0") int searchPage,
                        Model model) {
        populateListModel(q, itemType, reportPage, searchPage, model);
        return "items/list";
    }

    @GetMapping("/items/list")
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) LostItemType itemType,
                       @RequestParam(defaultValue = "0") int reportPage,
                       @RequestParam(defaultValue = "0") int searchPage,
                       Model model) {
        populateListModel(q, itemType, reportPage, searchPage, model);
        return "items/list";
    }

    @GetMapping("/items/{id}")
    public String detail(@PathVariable Long id, Model model) {
        populatePublicDetailModel(id, model, new CommentCreateForm());
        return "items/detail";
    }

    @GetMapping("/items/new")
    public String createForm(Model model, HttpSession session) {
        model.addAttribute("form", new LostItemCreateForm());
        populateCreateModel(model, session);
        return "items/create";
    }

    @PostMapping("/items")
    public String create(@Valid @ModelAttribute("form") LostItemCreateForm form,
                         BindingResult bindingResult,
                         @RequestParam("imageFile") MultipartFile imageFile,
                         @RequestParam("createToken") String createToken,
                         Model model,
                         HttpSession session) {
        model.addAttribute("categories", LostItemCategory.values());
        model.addAttribute("itemTypes", LostItemType.values());

        if (bindingResult.hasErrors()) {
            keepOrRefreshCreateToken(model, session, createToken);
            return "items/create";
        }

        if (!consumeCreateToken(session, createToken)) {
            return "redirect:/items/list";
        }

        try {
            lostItemService.createAnonymousItem(form, imageFile);
        } catch (IllegalArgumentException e) {
            model.addAttribute("fileError", e.getMessage());
            issueCreateToken(model, session);
            return "items/create";
        }
        return "redirect:/items/list";
    }

    @PostMapping("/items/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @Valid @ModelAttribute("commentForm") CommentCreateForm commentForm,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        if (bindingResult.hasErrors()) {
            populatePublicDetailModel(id, model, commentForm);
            return "items/detail";
        }

        try {
            lostItemService.addComment(id, userDetails.getUsername(), commentForm);
            return "redirect:/items/" + id;
        } catch (IllegalArgumentException e) {
            model.addAttribute("commentError", e.getMessage());
            populatePublicDetailModel(id, model, commentForm);
            return "items/detail";
        }
    }

    @PostMapping("/items/{itemId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long itemId,
                                @PathVariable Long commentId,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        try {
            lostItemService.deleteCommentByAuthor(itemId, commentId, userDetails.getUsername());
            return "redirect:/items/" + itemId;
        } catch (IllegalArgumentException e) {
            model.addAttribute("commentDeleteError", e.getMessage());
            populatePublicDetailModel(itemId, model, new CommentCreateForm());
            return "items/detail";
        }
    }

    private void populateCreateModel(Model model, HttpSession session) {
        model.addAttribute("categories", LostItemCategory.values());
        model.addAttribute("itemTypes", LostItemType.values());
        issueCreateToken(model, session);
    }

    private void issueCreateToken(Model model, HttpSession session) {
        String token = UUID.randomUUID().toString();
        synchronized (WebUtils.getSessionMutex(session)) {
            session.setAttribute(CREATE_ITEM_TOKEN_SESSION_KEY, token);
        }
        model.addAttribute("createToken", token);
    }

    private void keepOrRefreshCreateToken(Model model, HttpSession session, String createToken) {
        if (hasCurrentCreateToken(session, createToken)) {
            model.addAttribute("createToken", createToken);
            return;
        }
        issueCreateToken(model, session);
    }

    private boolean hasCurrentCreateToken(HttpSession session, String createToken) {
        synchronized (WebUtils.getSessionMutex(session)) {
            Object storedToken = session.getAttribute(CREATE_ITEM_TOKEN_SESSION_KEY);
            return storedToken instanceof String token && token.equals(createToken);
        }
    }

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

    private void populateListModel(String keyword,
                                   LostItemType itemType,
                                   int reportPage,
                                   int searchPage,
                                   Model model) {
        Page<LostItemListDto> reportPageData = lostItemService.getApprovedItemsByCategory(LostItemCategory.REPORT, itemType, keyword, reportPage);
        Page<LostItemListDto> searchPageData = lostItemService.getApprovedItemsByCategory(LostItemCategory.SEARCH, itemType, keyword, searchPage);

        model.addAttribute("reportPageData", reportPageData);
        model.addAttribute("searchPageData", searchPageData);
        model.addAttribute("reportItems", reportPageData.getContent());
        model.addAttribute("searchItems", searchPageData.getContent());
        List<LostItemListDto> mapItems = Stream.concat(reportPageData.getContent().stream(), searchPageData.getContent().stream())
                .filter(item -> item.getLatitude() != null && item.getLongitude() != null)
                .toList();
        model.addAttribute("mapItems", mapItems);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedItemType", itemType);
        model.addAttribute("itemTypes", LostItemType.values());
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
