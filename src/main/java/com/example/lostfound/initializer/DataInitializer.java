package com.example.lostfound.initializer;

import com.example.lostfound.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final MemberService memberService;

    @Bean
    public ApplicationRunner initAdmin() {
        return args -> memberService.createAdminIfAbsent();
    }
}
