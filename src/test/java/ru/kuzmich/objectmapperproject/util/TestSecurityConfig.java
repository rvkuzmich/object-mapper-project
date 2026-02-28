package ru.kuzmich.objectmapperproject.util;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@TestConfiguration
public class TestSecurityConfig {

    @Bean
    @Primary
    public UserDetailsService testUserDetailsService() {
        return new InMemoryUserDetailsManager(
            User.builder()
                .username("testuser")
                .password("{noop}password")
                .roles("USER")
                .build(),
            User.builder()
                .username("testmoderator")
                .password("{noop}password")
                .roles("MODERATOR")
                .build(),
            User.builder()
                .username("testadmin")
                .password("{noop}password")
                .roles("SUPER_ADMIN")
                .build()
        );
    }
}
