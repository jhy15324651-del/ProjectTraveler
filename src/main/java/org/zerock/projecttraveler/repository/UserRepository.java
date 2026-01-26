package org.zerock.projecttraveler.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.zerock.projecttraveler.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    List<User> findByRole(User.Role role);

    List<User> findByEnabledTrue();
}
