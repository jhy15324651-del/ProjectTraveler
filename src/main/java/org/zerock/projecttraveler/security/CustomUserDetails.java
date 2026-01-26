package org.zerock.projecttraveler.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class CustomUserDetails extends User {

    private final Long id;
    private final String fullName;
    private final String email;

    public CustomUserDetails(Long id, String username, String password, String fullName, String email,
                            boolean enabled, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, enabled, true, true, true, authorities);
        this.id = id;
        this.fullName = fullName;
        this.email = email;
    }

    public boolean isAdmin() {
        return getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
