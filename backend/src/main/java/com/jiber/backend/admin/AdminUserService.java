package com.jiber.backend.admin;

import com.jiber.backend.common.PageMetadata;
import com.jiber.backend.common.error.ApiException;
import com.jiber.backend.common.error.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final AdminUserMapper adminUserMapper;

    public AdminUserService(AdminUserMapper adminUserMapper) {
        this.adminUserMapper = adminUserMapper;
    }

    public AdminUserListResponse listUsers(AdminUserListRequest request) {
        var page = request.effectivePage();
        var size = request.effectiveSize();
        var rows = adminUserMapper.findUsers(request, size, page * size);
        var total = adminUserMapper.countUsers(request);
        return new AdminUserListResponse(
                rows.stream().map(this::toResponse).toList(),
                pageMetadata(page, size, total)
        );
    }

    @Transactional
    public AdminUserMutationResponse updateRole(Long userId, AdminUserRoleUpdateRequest request, Long actorUserId) {
        if (actorUserId != null && userId.equals(actorUserId) && request.role() != AdminUserRole.ADMIN) {
            throw new ApiException(ErrorCode.ADMIN_SELF_UPDATE_NOT_ALLOWED);
        }
        var updated = adminUserMapper.updateRole(userId, request.role().name());
        if (updated == 0) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return new AdminUserMutationResponse(findRequired(userId), "회원 권한을 변경했습니다.");
    }

    @Transactional
    public AdminUserMutationResponse updateEnabled(Long userId, AdminUserEnabledUpdateRequest request, Long actorUserId) {
        if (actorUserId != null && userId.equals(actorUserId) && Boolean.FALSE.equals(request.enabled())) {
            throw new ApiException(ErrorCode.ADMIN_SELF_UPDATE_NOT_ALLOWED);
        }
        var updated = adminUserMapper.updateEnabled(userId, request.enabled());
        if (updated == 0) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return new AdminUserMutationResponse(findRequired(userId), "회원 상태를 변경했습니다.");
    }

    private AdminUserSummaryResponse findRequired(Long userId) {
        var row = adminUserMapper.findById(userId);
        if (row == null) {
            throw new ApiException(ErrorCode.USER_NOT_FOUND);
        }
        return toResponse(row);
    }

    private AdminUserSummaryResponse toResponse(AdminUserRow row) {
        return new AdminUserSummaryResponse(
                row.userId(),
                row.email(),
                row.displayName(),
                AdminUserRole.valueOf(row.role()),
                Boolean.TRUE.equals(row.enabled()),
                row.lastLoginAt(),
                row.createdAt(),
                row.updatedAt()
        );
    }

    private PageMetadata pageMetadata(int page, int size, long total) {
        if (total == 0) {
            return PageMetadata.empty(page, size);
        }
        return new PageMetadata(page, size, total, (int) Math.ceil((double) total / size));
    }
}
