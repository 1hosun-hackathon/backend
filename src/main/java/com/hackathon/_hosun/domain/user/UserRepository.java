package com.hackathon._hosun.domain.user;

import com.hackathon._hosun.domain.user.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByLoginId(String loginId);

    Optional<User> findByLoginIdAndDeletedAtIsNull(String loginId);
}
