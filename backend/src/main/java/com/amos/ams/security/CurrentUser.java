package com.amos.ams.security;

import com.amos.ams.domain.User;
import com.amos.ams.exception.ApiException;
import com.amos.ams.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User require() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw ApiException.forbidden("Not authenticated");
        }
        return users.findByUsername(auth.getName())
                .orElseThrow(() -> ApiException.forbidden("User not found"));
    }

    public boolean hasPermission(String code) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(code) || a.getAuthority().equals("ROLE_ADMIN"));
    }
}
