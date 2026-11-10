package com.hackathon._hosun.domain.user.service;

import com.hackathon._hosun.domain.training.entity.TrainingStatus;
import com.hackathon._hosun.domain.training.repository.TrainingRepository;
import com.hackathon._hosun.domain.user.dto.UserMeResponse;
import com.hackathon._hosun.domain.user.entity.User;
import com.hackathon._hosun.domain.user.repository.UserRepository;
import com.hackathon._hosun.global.exception.BusinessException;
import com.hackathon._hosun.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TrainingRepository trainingRepository;

    public UserMeResponse getMe(Long userId) { //내 정보 조회
        User user = findActiveUser(userId);
        long completedCount = trainingRepository.countByUser_IdAndStatusAndDeletedAtIsNull(
                userId, TrainingStatus.COMPLETED);

        return new UserMeResponse(user.getId(), user.getLoginId(), completedCount, user.getCreatedAt());
    }

    @Transactional
    public void withdraw(Long userId) { //유저 삭제
        User user = findActiveUser(userId);
        user.delete();
        user.clearRefreshToken();
    }

    private User findActiveUser(Long userId) { //repository로 유저 찾기
        return userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
    }
}
