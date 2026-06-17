package com.jiber.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
        "spring.datasource.url=jdbc:h2:mem:auth_user_mapper;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;NON_KEYWORDS=USER,USERS",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "mybatis.mapper-locations=classpath:/mapper/**/*.xml",
        "mybatis.configuration.map-underscore-to-camel-case=true"
})
class AuthUserMapperMyBatisTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-06-15T07:00:00Z"), ZoneOffset.UTC);

    @Autowired
    private AuthUserMapper authUserMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpSchema() {
        jdbcTemplate.execute("DROP TABLE IF EXISTS users");
        jdbcTemplate.execute("""
                CREATE TABLE users (
                    user_id BIGINT NOT NULL AUTO_INCREMENT,
                    oauth_provider VARCHAR(20) NOT NULL,
                    provider_user_id VARCHAR(255) NOT NULL,
                    email VARCHAR(320),
                    display_name VARCHAR(100),
                    role VARCHAR(20) NOT NULL DEFAULT 'USER',
                    enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    last_login_at TIMESTAMP WITH TIME ZONE,
                    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (user_id),
                    UNIQUE (oauth_provider, provider_user_id)
                )
                """);
    }

    @Test
    void oauthProvisioningUsesDbBackedMapperAndMapsAuthUserRecord() {
        var service = LocalOAuth2UserProvisioningService.forTesting(authUserMapper, FIXED_CLOCK);

        var principal = service.provision(new OAuth2ProviderUser(
                OAuth2Provider.NAVER,
                "naver-user-1",
                "naver-user@example.com",
                "네이버 사용자"
        ));

        var byProvider = authUserMapper.findByProvider("NAVER", "naver-user-1");
        var byId = authUserMapper.findById(principal.userId());

        assertThat(byProvider).isNotNull();
        assertThat(byId).isNotNull();
        assertThat(byProvider).isEqualTo(byId);
        assertThat(byProvider.userId()).isNotNull();
        assertThat(byProvider.oauthProvider()).isEqualTo("NAVER");
        assertThat(byProvider.providerUserId()).isEqualTo("naver-user-1");
        assertThat(byProvider.email()).isEqualTo("naver-user@example.com");
        assertThat(byProvider.displayName()).isEqualTo("네이버 사용자");
        assertThat(byProvider.role()).isEqualTo("USER");
        assertThat(byProvider.enabled()).isTrue();
        assertThat(byProvider.lastLoginAt()).isNotNull();
        assertThat(byProvider.createdAt()).isNotNull();
        assertThat(byProvider.updatedAt()).isNotNull();

        assertThat(principal.roles()).containsExactly("USER");
        assertThat(principal.roles()).doesNotContain("ADMIN");
    }
}
