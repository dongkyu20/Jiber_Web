package com.jiber.backend.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminUserServiceTest {

    private AdminUserMapper adminUserMapper;
    private AdminUserService adminUserService;

    @BeforeEach
    void setUp() {
        adminUserMapper = mock(AdminUserMapper.class);
        adminUserService = new AdminUserService(adminUserMapper);
    }

    @Test
    void listUsersMapsRowsAndPagination() {
        var request = new AdminUserListRequest(0, 20, "admin", "ADMIN", true, null);
        when(adminUserMapper.findUsers(request, 20, 0)).thenReturn(List.of(row(1L, "admin@example.com", "관리자", "ADMIN", true)));
        when(adminUserMapper.countUsers(request)).thenReturn(1L);

        var response = adminUserService.listUsers(request);

        assertThat(response.items()).singleElement().satisfies(user -> {
            assertThat(user.userId()).isEqualTo(1L);
            assertThat(user.email()).isEqualTo("admin@example.com");
            assertThat(user.role()).isEqualTo(AdminUserRole.ADMIN);
            assertThat(user.enabled()).isTrue();
        });
        assertThat(response.page().totalElements()).isEqualTo(1L);
    }

    @Test
    void updateRoleBlocksSelfDemotion() {
        assertThatThrownBy(() -> adminUserService.updateRole(7L, new AdminUserRoleUpdateRequest(AdminUserRole.USER), 7L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADMIN_SELF_UPDATE_NOT_ALLOWED);
    }

    @Test
    void updateEnabledBlocksSelfDisable() {
        assertThatThrownBy(() -> adminUserService.updateEnabled(7L, new AdminUserEnabledUpdateRequest(false), 7L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ADMIN_SELF_UPDATE_NOT_ALLOWED);
    }

    @Test
    void updateRoleChangesTargetUser() {
        when(adminUserMapper.updateRole(2L, "ADMIN")).thenReturn(1);
        when(adminUserMapper.findById(2L)).thenReturn(row(2L, "user@example.com", "사용자", "ADMIN", true));

        var response = adminUserService.updateRole(2L, new AdminUserRoleUpdateRequest(AdminUserRole.ADMIN), 7L);

        assertThat(response.user().role()).isEqualTo(AdminUserRole.ADMIN);
        assertThat(response.message()).isEqualTo("회원 권한을 변경했습니다.");
        verify(adminUserMapper).updateRole(2L, "ADMIN");
    }

    @Test
    void updateEnabledThrowsUserNotFoundWhenNoRowChanged() {
        when(adminUserMapper.updateEnabled(404L, false)).thenReturn(0);

        assertThatThrownBy(() -> adminUserService.updateEnabled(404L, new AdminUserEnabledUpdateRequest(false), 7L))
                .isInstanceOf(ApiException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    private AdminUserRow row(Long userId, String email, String displayName, String role, Boolean enabled) {
        return new AdminUserRow(
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
