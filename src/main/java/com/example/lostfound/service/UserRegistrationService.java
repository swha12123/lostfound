package com.example.lostfound.service;

import com.example.lostfound.domain.entity.Member;
import com.example.lostfound.domain.enums.Role;
import com.example.lostfound.domain.repository.MemberRepository;
import com.example.lostfound.dto.MemberSignupForm;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistrationService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(MemberSignupForm form) {
        String username = form.getUsername().trim();
        String displayName = form.getDisplayName().trim();

        if (memberRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        Member member = Member.builder()
                .username(username)
                .password(passwordEncoder.encode(form.getPassword().trim()))
                .displayName(displayName)
                .role(Role.USER)
                .build();

        memberRepository.save(member);
    }
}
