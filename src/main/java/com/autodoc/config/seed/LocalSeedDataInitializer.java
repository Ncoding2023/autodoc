package com.autodoc.config.seed;

import com.autodoc.domain.team.Team;
import com.autodoc.domain.team.TeamRepository;
import com.autodoc.domain.template.DocumentTemplate;
import com.autodoc.domain.template.DocumentTemplateRepository;
import com.autodoc.domain.template.TemplateScope;
import com.autodoc.domain.template.WritingFormat;
import com.autodoc.domain.user.User;
import com.autodoc.domain.user.UserRepository;
import com.autodoc.domain.user.UserRole;
import com.autodoc.domain.user.UserStatus;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("local")
@ConditionalOnProperty(prefix = "app.seed", name = "enabled", havingValue = "true")
public class LocalSeedDataInitializer implements ApplicationRunner {

    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final DocumentTemplateRepository templateRepository;
    private static final String DEFAULT_PASSWORD = "autodoc1234";
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public LocalSeedDataInitializer(TeamRepository teamRepository, UserRepository userRepository,
                                    DocumentTemplateRepository templateRepository) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Team team = createTeam();
        createUsers(team.getId());
        createBasicTemplates();
    }

    private Team createTeam() {
        return teamRepository.findByName("AutoDoc 예시 팀")
                .orElseGet(() -> teamRepository.saveAndFlush(
                        new Team("AutoDoc 예시 팀", "local 프로필 전용 예시 팀")));
    }

    private void createUsers(Long teamId) {
        createUser(teamId, "admin@autodoc.local", "관리자", UserRole.ADMIN);
        createUser(teamId, "manager@autodoc.local", "매니저", UserRole.MANAGER);
        createUser(null, "user@autodoc.local", "개인 사용자", UserRole.USER);
    }

    private void createUser(Long teamId, String email, String name, UserRole role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(new User(teamId, email, passwordEncoder.encode(DEFAULT_PASSWORD), name, role, UserStatus.ACTIVE));
        }
    }

    private void createBasicTemplates() {
        createTemplate("기본 업무보고서", WritingFormat.DOCS, "업무보고서",
                "{\"sections\":[\"title\",\"bodyContent\",\"importantNotes\",\"cautions\"]}");
        createTemplate("기본 업무 목록", WritingFormat.SHEETS, "업무 목록",
                "{\"columns\":[{\"key\":\"task\",\"name\":\"업무\",\"type\":\"TEXT\"},{\"key\":\"dueDate\",\"name\":\"마감일\",\"type\":\"DATE\"},{\"key\":\"done\",\"name\":\"완료\",\"type\":\"BOOLEAN\"}]}");
    }

    private void createTemplate(String name, WritingFormat writingFormat, String documentType, String structureData) {
        if (!templateRepository.existsByTemplateScopeAndName(TemplateScope.BASIC, name)) {
            templateRepository.save(new DocumentTemplate(TemplateScope.BASIC, name, writingFormat, documentType,
                    structureData, "{\"fontFamily\":\"Noto Sans KR\",\"fontSize\":12,\"alignment\":\"LEFT\"}"));
        }
    }
}
