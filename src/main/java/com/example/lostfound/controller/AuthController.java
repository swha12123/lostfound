package com.example.lostfound.controller;

import com.example.lostfound.dto.MemberSignupForm;
import com.example.lostfound.service.UserRegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRegistrationService userRegistrationService;

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String redirect,
                            Model model) {
        model.addAttribute("redirect", redirect);
        return "auth/login";
    }

    @GetMapping("/signup")
    public String signupPage(@RequestParam(required = false) String redirect,
                             Model model) {
        model.addAttribute("form", new MemberSignupForm());
        model.addAttribute("redirect", redirect);
        return "auth/signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") MemberSignupForm form,
                         BindingResult bindingResult,
                         @RequestParam(required = false) String redirect,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        model.addAttribute("redirect", redirect);

        if (!form.getPassword().equals(form.getPasswordConfirm())) {
            bindingResult.rejectValue("passwordConfirm", "password.mismatch", "비밀번호 확인이 일치하지 않습니다.");
        }

        if (bindingResult.hasErrors()) {
            return "auth/signup";
        }

        try {
            userRegistrationService.register(form);
        } catch (IllegalArgumentException e) {
            bindingResult.rejectValue("username", "username.duplicate", e.getMessage());
            return "auth/signup";
        }

        redirectAttributes.addAttribute("signupSuccess", true);
        if (redirect != null && redirect.startsWith("/") && !redirect.startsWith("//")) {
            redirectAttributes.addAttribute("redirect", redirect);
        }
        return "redirect:/login";
    }
}
