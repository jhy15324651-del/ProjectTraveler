package org.zerock.projecttraveler.controller.common;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.zerock.projecttraveler.security.CustomUserDetails;
import org.zerock.projecttraveler.security.SecurityUtils;

@ControllerAdvice
public class GlobalModelAttributes {

    @ModelAttribute("username")
    public String username() {
        CustomUserDetails user = SecurityUtils.getCurrentUserDetails().orElse(null);
        return (user != null && user.getFullName() != null && !user.getFullName().isBlank())
                ? user.getFullName()
                : "사용자";
    }

    @ModelAttribute("isAdmin")
    public boolean isAdmin() {
        return SecurityUtils.isAdmin();
    }
}
