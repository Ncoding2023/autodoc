package com.autodoc.domain.user;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.team.TeamRepository;
import com.autodoc.domain.user.dto.UserCreateRequest;
import com.autodoc.domain.user.dto.UserResponse;
import com.autodoc.domain.user.dto.UserUpdateRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, TeamRepository teamRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.teamRepository = teamRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse signUp(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        validateTeamExists(request.teamId());

        User user = new User(request.teamId(), request.email(), passwordEncoder.encode(request.password()), request.name(),
                UserRole.USER, UserStatus.ACTIVE);
        return UserResponse.from(userRepository.save(user));
    }

    public UserResponse getUser(Long userId) {
        return UserResponse.from(findUser(userId));
    }

    public UserResponse getUserForCurrentUser(Long userId, Long currentUserId) {
        validateOwnership(userId, currentUserId);
        return getUser(userId);
    }

    @Transactional
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        validateTeamExists(request.teamId());

        User user = findUser(userId);
        user.updateProfile(request.teamId(), request.name(), user.getRole(), user.getStatus());
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse updateUserForCurrentUser(Long userId, Long currentUserId, UserUpdateRequest request) {
        validateOwnership(userId, currentUserId);
        return updateUser(userId, request);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateTeamExists(Long teamId) {
        if (teamId != null && !teamRepository.existsById(teamId)) {
            throw new BusinessException(ErrorCode.TEAM_NOT_FOUND);
        }
    }

    private void validateOwnership(Long userId, Long currentUserId) {
        if (!userId.equals(currentUserId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }
    }
}
