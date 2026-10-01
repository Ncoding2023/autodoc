package com.autodoc.domain.team;

import com.autodoc.domain.auth.AuthService;
import com.autodoc.domain.team.dto.TeamCreateRequest;
import com.autodoc.domain.team.dto.TeamResponse;
import com.autodoc.domain.team.dto.TeamUpdateRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;
    private final AuthService authService;

    public TeamController(TeamService teamService, AuthService authService) {
        this.teamService = teamService;
        this.authService = authService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody TeamCreateRequest request, HttpSession session) {
        authService.getCurrentUser(session);
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.createTeam(request));
    }

    @GetMapping
    public List<TeamResponse> getTeams(HttpSession session) {
        authService.getCurrentUser(session);
        return teamService.getTeams();
    }

    @GetMapping("/{teamId}")
    public TeamResponse getTeam(@PathVariable Long teamId, HttpSession session) {
        authService.getCurrentUser(session);
        return teamService.getTeam(teamId);
    }

    @PatchMapping("/{teamId}")
    public TeamResponse updateTeam(@PathVariable Long teamId, @Valid @RequestBody TeamUpdateRequest request, HttpSession session) {
        authService.getCurrentUser(session);
        return teamService.updateTeam(teamId, request);
    }

    @DeleteMapping("/{teamId}")
    public ResponseEntity<Void> deleteTeam(@PathVariable Long teamId, HttpSession session) {
        authService.getCurrentUser(session);
        teamService.deleteTeam(teamId);
        return ResponseEntity.noContent().build();
    }
}
