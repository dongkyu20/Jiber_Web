package com.jiber.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.boot.test.autoconfigure.MybatisTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

@MybatisTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:admin_user_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER,USERS",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "mybatis.mapper-locations=classpath:/mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
class AdminUserMapperMyBatisTest {

    @Autowired
    private AdminUserMapper adminUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    user_id BIGINT AUTO_INCREMENT,
                    email VARCHAR(320),
                    password_hash VARCHAR(255),
                    display_name VARCHAR(100),
                    role VARCHAR(20) NOT NULL DEFAULT 'USER',
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    last_login_at TIMESTAMP(6),
                    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
                    PRIMARY KEY (user_id)
                )
                """);
    }

    @Test
    void findUsersFiltersByKeywordRoleAndEnabled() {
        insertUser(1L, "admin@example.com", "관리자", "ADMIN", true);
        insertUser(2L, "user@example.com", "일반 사용자", "USER", true);
        insertUser(3L, "disabled@example.com", "비활성", "USER", false);

        var request = new AdminUserListRequest(0, 20, "admin", "ADMIN", true, "createdAt,desc");

        var users = adminUserMapper.findUsers(request, 20, 0);

        assertThat(users).extracting(AdminUserRow::userId).containsExactly(1L);
        assertThat(adminUserMapper.countUsers(request)).isEqualTo(1);
    }

    @Test
    void updateRoleAndEnabledMutateSingleUser() {
        insertUser(2L, "user@example.com", "일반 사용자", "USER", true);

        assertThat(adminUserMapper.updateRole(2L, "ADMIN")).isEqualTo(1);
        assertThat(adminUserMapper.updateEnabled(2L, false)).isEqualTo(1);

        var updated = adminUserMapper.findById(2L);
        assertThat(updated.role()).isEqualTo("ADMIN");
        assertThat(updated.enabled()).isFalse();
    }

    private void insertUser(Long userId, String email, String displayName, String role, boolean enabled) {
        jdbcTemplate.update("""
                        INSERT INTO users (
                            user_id, email, password_hash, display_name, role, enabled, last_login_at, created_at, updated_at
                        )
                        VALUES (?, ?, NULL, ?, ?, ?, ?, ?, ?)
                        """,
                userId,
                email,
                displayName,
                role,
                enabled,
                OffsetDateTime.parse("2026-06-24T09:00:00+09:00"),
                OffsetDateTime.parse("2026-06-20T09:00:00+09:00"),
                OffsetDateTime.parse("2026-06-24T09:00:00+09:00")
        );
    }
}
