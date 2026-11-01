package com.ecommerce.order_service.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final WebClient.Builder webClientBuilder;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            // Call user service to validate user
            Boolean isValid = webClientBuilder.build()
                    .get()
                    .uri("http://localhost:8081/api/users/validate?email=" + email)
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();

            if (isValid == null || !isValid) {
                throw new UsernameNotFoundException("User not found with email: " + email);
            }

            // Return a simple user details object
            return org.springframework.security.core.userdetails.User.builder()
                    .username(email)
                    .password("") // Password not needed for JWT validation
                    .authorities(Collections.emptyList())
                    .build();
        } catch (Exception e) {
            log.error("Error validating user: {}", e.getMessage());
            throw new UsernameNotFoundException("User validation failed", e);
        }
    }
}
