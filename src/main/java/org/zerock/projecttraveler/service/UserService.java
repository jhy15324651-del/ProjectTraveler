package org.zerock.projecttraveler.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zerock.projecttraveler.entity.User;
import org.zerock.projecttraveler.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     */
    @Transactional
    public User register(String username, String password, String email, String fullName) {
        // 중복 체크
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (email != null && !email.isBlank() && userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName != null ? fullName : username)
                .role(User.Role.USER)
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * 관리자 계정 생성
     */
    @Transactional
    public User registerAdmin(String username, String password, String email, String fullName) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .fullName(fullName != null ? fullName : username)
                .role(User.Role.ADMIN)
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    /**
     * 사용자 조회
     */
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public List<User> findAllUsers() {
        return userRepository.findByRole(User.Role.USER);
    }

    public List<User> findAllEnabledUsers() {
        return userRepository.findByEnabledTrue();
    }

    /**
     * 비밀번호 변경
     */
    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
    }

    /**
     * 프로필 업데이트
     */
    @Transactional
    public void updateProfile(Long userId, String fullName, String email) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName);
        }
        if (email != null && !email.isBlank()) {
            // 다른 사용자가 이미 사용 중인 이메일인지 체크
            userRepository.findByUsername(user.getUsername())
                    .filter(u -> !u.getId().equals(userId))
                    .ifPresent(u -> {
                        throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
                    });
            user.setEmail(email);
        }
    }
}
