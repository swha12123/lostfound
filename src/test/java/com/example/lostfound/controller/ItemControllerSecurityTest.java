package com.example.lostfound.controller;

import com.example.lostfound.config.SecurityConfig;
import com.example.lostfound.dto.CommentCreateForm;
import com.example.lostfound.service.CustomUserDetailsService;
import com.example.lostfound.service.LostItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
@Import(SecurityConfig.class)
class ItemControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LostItemService lostItemService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void unauthenticatedCommentPostRedirectsToLogin() throws Exception {
        mockMvc.perform(post("/items/1/comments")
                        .with(csrf())
                        .param("content", "댓글 테스트"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/login")));
    }

    @Test
    void authenticatedCommentPostCallsService() throws Exception {
        mockMvc.perform(post("/items/1/comments")
                        .with(user("member").roles("USER"))
                        .with(csrf())
                        .param("content", "댓글 테스트"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/items/1"));

        verify(lostItemService).addComment(eq(1L), eq("member"), any(CommentCreateForm.class));
    }
}
