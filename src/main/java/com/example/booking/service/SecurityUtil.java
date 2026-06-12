package com.example.booking.service;

import com.example.booking.security.AppUserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtil {
    private SecurityUtil() {}

    public static AppUserPrincipal currentPrincipal() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !(a.getPrincipal() instanceof AppUserPrincipal p)) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }
        return p;
    }

    public static Long currentUserId() {
        return currentPrincipal().getId();
    }

    public static boolean hasRole(String role) {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null) return false;
        return a.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals("ROLE_" + role));
    }
}
