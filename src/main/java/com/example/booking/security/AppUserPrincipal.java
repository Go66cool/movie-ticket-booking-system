package com.example.booking.security;

import com.example.booking.domain.User;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class AppUserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final String role;

    public AppUserPrincipal(User u) {
        this.id = u.getId();
        this.email = u.getEmail();
        this.password = u.getPasswordHash();
        this.role = u.getRole().name();
    }

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
