package com.autodoc.domain.team;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "teams", uniqueConstraints = @UniqueConstraint(name = "uk_teams_team_name", columnNames = "team_name"))
public class Team {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) @Column(name = "team_id") private Long id;
    @Column(name = "team_name", nullable = false, length = 100) private String name;
    @Column(length = 255) private String description;
    @Column(name = "created_at", nullable = false, updatable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    protected Team() {}
    public Team(String name, String description) { this.name = name; this.description = description; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public void update(String name, String description) { this.name = name; this.description = description; }
    @PrePersist void prePersist() { LocalDateTime now = LocalDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void preUpdate() { updatedAt = LocalDateTime.now(); }
}
