package com.hackathon._hosun.domain.user.repository;

import com.hackathon._hosun.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginIdAndDeletedAtIsNull(String loginId);

    Optional<User> findByRefreshTokenHash(String refreshTokenHash);
}
