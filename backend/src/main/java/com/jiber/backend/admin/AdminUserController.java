package com.jiber.backend.admin;

import com.jiber.backend.auth.dto.AuthUserPrincipal;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public AdminUserListResponse listUsers(@Valid @ParameterObject @ModelAttribute AdminUserListRequest request) {
        return adminUserService.listUsers(request);
    }

    @PatchMapping("/{userId}/role")
    public AdminUserMutationResponse updateRole(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserRoleUpdateRequest request
    ) {
        return adminUserService.updateRole(userId, request, authenticatedUserId(principal));
    }

    @PatchMapping("/{userId}/enabled")
    public AdminUserMutationResponse updateEnabled(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long userId,
            @Valid @RequestBody AdminUserEnabledUpdateRequest request
    ) {
        return adminUserService.updateEnabled(userId, request, authenticatedUserId(principal));
    }

    private Long authenticatedUserId(AuthUserPrincipal principal) {
        return principal == null ? null : principal.userId();
    }
}
