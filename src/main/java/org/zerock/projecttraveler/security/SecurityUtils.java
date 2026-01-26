package org.zerock.projecttraveler.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Security Context에서 현재 사용자 정보를 가져오는 유틸리티 클래스
 * 중요: 학습기록 API에서 userId를 클라이언트로부터 받지 않고 서버에서 강제로 가져옴
 */
public class SecurityUtils {

    /**
     * 현재 로그인한 사용자의 ID 반환
     */
    public static Optional<Long> getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return Optional.of(((CustomUserDetails) auth.getPrincipal()).getId());
        }
        return Optional.empty();
    }

    /**
     * 현재 로그인한 사용자의 ID 반환 (없으면 예외)
     */
    public static Long getCurrentUserIdOrThrow() {
        return getCurrentUserId()
                .orElseThrow(() -> new IllegalStateException("로그인이 필요합니다."));
    }

    /**
     * 현재 로그인한 사용자의 username 반환
     */
    public static Optional<String> getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return Optional.of(((CustomUserDetails) auth.getPrincipal()).getUsername());
        }
        return Optional.empty();
    }

    /**
     * 현재 로그인한 사용자의 CustomUserDetails 반환
     */
    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails) {
            return Optional.of((CustomUserDetails) auth.getPrincipal());
        }
        return Optional.empty();
    }

    /**
     * 현재 사용자가 관리자인지 확인
     */
    public static boolean isAdmin() {
        return getCurrentUserDetails()
                .map(CustomUserDetails::isAdmin)
                .orElse(false);
    }

    /**
     * 현재 사용자가 인증되었는지 확인
     */
    public static boolean isAuthenticated() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() &&
               auth.getPrincipal() instanceof CustomUserDetails;
    }
}
