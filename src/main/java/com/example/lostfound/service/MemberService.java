package com.example.lostfound.service;

import com.example.lostfound.domain.entity.Member;
import com.example.lostfound.domain.enums.Role;
import com.example.lostfound.domain.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    public void createAdminIfAbsent() {
        if (!memberRepository.existsByUsername("admin")) {
            Member admin = Member.builder()
                    .username("admin")
                    .password(passwordEncoder.encode("admin123"))
                    .displayName("관리자")
                    .role(Role.ADMIN)
                    .build();
            memberRepository.save(admin);
        }
    }
}
