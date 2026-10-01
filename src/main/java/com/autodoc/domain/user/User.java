package com.autodoc.domain.user;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "user_id")
    private Long id;
    @Column(name = "team_id") private Long teamId;
    @Column(nullable = false, length = 255) private String email;
    @Column(nullable = false, length = 255) private String password;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserRole role = UserRole.USER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserStatus status = UserStatus.ACTIVE;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    protected User() {}
    public User(Long teamId, String email, String password, String name, UserRole role, UserStatus status) {
        this.teamId = teamId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
        this.status = status;
    }
    public Long getId() { return id; }
    public Long getTeamId() { return teamId; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getName() { return name; }
    public UserRole getRole() { return role; }
    public UserStatus getStatus() { return status; }
    public void updateProfile(Long teamId, String name, UserRole role, UserStatus status) {
        this.teamId = teamId;
        this.name = name;
        this.role = role;
        this.status = status;
    }
    @PrePersist void prePersist() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}
