package com.ragchat.util;

import com.ragchat.model.User;
import com.ragchat.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/**
 * Convenience utility to extract the authenticated user's ID
 * from Spring Security context in any controller.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Returns the MongoDB _id of the currently authenticated user.
     */
    public String getCurrentUserId() {
        String email = getCurrentEmail();
        return userRepository.findByEmail(email)
                .map(User::getId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user not found in DB"));
    }

    /**
     * Returns the email of the currently authenticated user.
     */
    public String getCurrentEmail() {
        Object principal = SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }
        return principal.toString();
    }
}
