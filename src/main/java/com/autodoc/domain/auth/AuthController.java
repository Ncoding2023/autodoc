package com.autodoc.domain.auth;

import com.autodoc.domain.auth.dto.LoginRequest;
import com.autodoc.domain.user.dto.UserResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public UserResponse login(@Valid @RequestBody LoginRequest request, HttpSession session,
                              HttpServletRequest servletRequest, HttpServletResponse servletResponse) {
        UserResponse user = authService.login(request, session);
        servletRequest.changeSessionId();
        writeSessionCookie(servletRequest, servletResponse, session.getId(),
                request.rememberMe() ? AuthService.REMEMBER_ME_SECONDS : -1);
        return user;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session, HttpServletRequest request, HttpServletResponse response) {
        authService.logout(session);
        writeSessionCookie(request, response, "", 0);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/me")
    public UserResponse getCurrentUser(HttpSession session) {
        return authService.getCurrentUser(session);
    }

    private void writeSessionCookie(HttpServletRequest request, HttpServletResponse response, String value, int maxAge) {
        var cookieConfig = request.getServletContext().getSessionCookieConfig();
        String name = cookieConfig.getName() == null ? "JSESSIONID" : cookieConfig.getName();
        String path = cookieConfig.getPath() == null
                ? (request.getContextPath().isEmpty() ? "/" : request.getContextPath()) : cookieConfig.getPath();
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .path(path).domain(cookieConfig.getDomain()).httpOnly(true)
                .secure(request.isSecure() || cookieConfig.isSecure()).sameSite("Lax").maxAge(maxAge).build();
        response.setHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
