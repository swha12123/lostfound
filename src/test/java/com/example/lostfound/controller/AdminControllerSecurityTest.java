package com.example.lostfound.controller;

import com.example.lostfound.config.SecurityConfig;
import com.example.lostfound.dto.CommentCreateForm;
import com.example.lostfound.dto.LostItemDetailDto;
import com.example.lostfound.dto.LostItemUpdateForm;
import com.example.lostfound.service.CustomUserDetailsService;
import com.example.lostfound.service.LostItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LostItemService lostItemService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void nonAdminCannotOpenEditForm() throws Exception {
        mockMvc.perform(get("/admin/items/1/edit")
                        .with(user("member").roles("USER")))
                .andExpect(status().isForbidden());

        verify(lostItemService, never()).getAdminUpdateForm(any());
    }

    @Test
    void adminCanOpenEditForm() throws Exception {
        when(lostItemService.getAdminUpdateForm(1L)).thenReturn(new LostItemUpdateForm());
        when(lostItemService.getItemDetail(1L)).thenReturn(LostItemDetailDto.builder()
                .id(1L)
                .title("수정 대상")
                .comments(java.util.List.of())
                .imagePaths(java.util.List.of())
                .build());

        mockMvc.perform(get("/admin/items/1/edit")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(lostItemService).getAdminUpdateForm(1L);
        verify(lostItemService).getItemDetail(1L);
    }

    @Test
    void nonAdminCannotSubmitEdit() throws Exception {
        mockMvc.perform(post("/admin/items/1/edit")
                        .with(user("member").roles("USER"))
                        .with(csrf())
                        .param("category", "REPORT")
                        .param("itemType", "WALLET")
                        .param("title", "수정 제목")
                        .param("description", "수정 설명")
                        .param("locationName", "학생회관 앞")
                        .param("contactInfo", "010-1234-5678")
                        .param("latitude", "37.5052")
                        .param("longitude", "126.9571"))
                .andExpect(status().isForbidden());

        verify(lostItemService, never()).updateItemByAdmin(eq(1L), any(LostItemUpdateForm.class));
    }

    @Test
    void adminCanSubmitEdit() throws Exception {
        mockMvc.perform(post("/admin/items/1/edit")
                        .with(user("admin").roles("ADMIN"))
                        .with(csrf())
                        .param("category", "REPORT")
                        .param("itemType", "WALLET")
                        .param("title", "수정 제목")
                        .param("description", "수정 설명")
                        .param("locationName", "학생회관 앞")
                        .param("contactInfo", "010-1234-5678")
                        .param("latitude", "37.5052")
                        .param("longitude", "126.9571"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/items/1"));

        verify(lostItemService).updateItemByAdmin(eq(1L), any(LostItemUpdateForm.class));
    }
}
