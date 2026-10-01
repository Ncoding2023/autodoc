package com.autodoc.domain.user;

import com.autodoc.support.LocalPostgreSqlRepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=validate", "spring.test.database.replace=none"})
class UserRepositoryTest extends LocalPostgreSqlRepositoryTestSupport {
    private static final Logger log = LoggerFactory.getLogger(UserRepositoryTest.class);
    @Autowired private UserRepository userRepository;

    @Test void createsUser() {
        User saved = userRepository.saveAndFlush(user("repository-create-user@example.test"));
        log.info("회원 생성 완료: id={}", saved.getId());
        assertThat(saved.getId()).isNotNull();
    }

    @Test void readsUserAndChecksEmailExistence() {
        User saved = userRepository.saveAndFlush(user("repository-read-user@example.test"));
        assertThat(userRepository.findById(saved.getId())).isPresent();
        assertThat(userRepository.existsByEmail("repository-read-user@example.test")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.test")).isFalse();
        log.info("회원 조회 및 이메일 중복 조회 완료: id={}", saved.getId());
    }

    @Test void updatesUser() {
        User saved = userRepository.saveAndFlush(user("repository-update-user@example.test"));
        User found = userRepository.findById(saved.getId()).orElseThrow();
        found.updateProfile(null, "updated-user", UserRole.MANAGER, UserStatus.INACTIVE);
        userRepository.flush();
        assertThat(userRepository.findById(saved.getId())).get()
                .extracting(User::getName, User::getRole, User::getStatus)
                .containsExactly("updated-user", UserRole.MANAGER, UserStatus.INACTIVE);
        log.info("회원 수정 완료: id={}", saved.getId());
    }

    @Test void deletesUser() {
        User saved = userRepository.saveAndFlush(user("repository-delete-user@example.test"));
        userRepository.deleteById(saved.getId());
        userRepository.flush();
        assertThat(userRepository.findById(saved.getId())).isEmpty();
        log.info("회원 삭제 완료: id={}", saved.getId());
    }

    private User user(String email) {
        return new User(null, email, "encoded-password", "test-user", UserRole.USER, UserStatus.ACTIVE);
    }
}
