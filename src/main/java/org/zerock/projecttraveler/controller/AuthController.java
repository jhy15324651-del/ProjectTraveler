package org.zerock.projecttraveler.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.zerock.projecttraveler.dto.ApiResponse;
import org.zerock.projecttraveler.entity.User;
import org.zerock.projecttraveler.service.UserService;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 로그인 페이지
     */
    @GetMapping({"/", "/login"})
    public String loginPage(@RequestParam(value = "error", required = false) String error,
                           @RequestParam(value = "logout", required = false) String logout,
                           @RequestParam(value = "expired", required = false) String expired,
                           Model model) {
        if (error != null) {
            model.addAttribute("errorMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
        }
        if (logout != null) {
            model.addAttribute("logoutMessage", "로그아웃되었습니다.");
        }
        if (expired != null) {
            model.addAttribute("expiredMessage", "세션이 만료되었습니다. 다시 로그인해주세요.");
        }
        return "index";
    }

    /**
     * 회원가입 페이지
     */
    @GetMapping("/register")
    public String registerPage() {
        return "register";
    }

    /**
     * 회원가입 API
     */
    @PostMapping("/api/auth/register")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            userService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getEmail(),
                    request.getFullName()
            );
            return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    @Data
    public static class RegisterRequest {
        @NotBlank(message = "아이디는 필수입니다.")
        private String username;

        @NotBlank(message = "비밀번호는 필수입니다.")
        private String password;

        private String email;
        private String fullName;
    }
}
