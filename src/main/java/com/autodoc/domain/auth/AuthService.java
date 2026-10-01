package com.autodoc.domain.auth;

import com.autodoc.common.exception.BusinessException;
import com.autodoc.common.exception.ErrorCode;
import com.autodoc.domain.auth.dto.LoginRequest;
import com.autodoc.domain.user.User;
import com.autodoc.domain.user.UserRepository;
import com.autodoc.domain.user.UserStatus;
import com.autodoc.domain.user.dto.UserResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserResponse login(LoginRequest request, HttpSession session) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException(ErrorCode.INACTIVE_USER);
        }

        session.setAttribute(AuthSession.LOGIN_USER_ID, user.getId());
        return UserResponse.from(user);
    }

    public void logout(HttpSession session) {
        session.invalidate();
    }

    public UserResponse getCurrentUser(HttpSession session) {
        Object value = session.getAttribute(AuthSession.LOGIN_USER_ID);
        if (!(value instanceof Long userId)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        return UserResponse.from(user);
    }
}
