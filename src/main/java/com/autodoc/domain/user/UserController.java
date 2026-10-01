package com.autodoc.domain.user;

import com.autodoc.domain.auth.AuthService;
import com.autodoc.domain.user.dto.UserCreateRequest;
import com.autodoc.domain.user.dto.UserResponse;
import com.autodoc.domain.user.dto.UserUpdateRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> signUp(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signUp(request));
    }

    @GetMapping("/{userId}")
    public UserResponse getUser(@PathVariable Long userId, HttpSession session) {
        UserResponse currentUser = authService.getCurrentUser(session);
        return userService.getUserForCurrentUser(userId, currentUser.id());
    }

    @PatchMapping("/{userId}")
    public UserResponse updateUser(@PathVariable Long userId, @Valid @RequestBody UserUpdateRequest request, HttpSession session) {
        UserResponse currentUser = authService.getCurrentUser(session);
        return userService.updateUserForCurrentUser(userId, currentUser.id(), request);
    }
}
