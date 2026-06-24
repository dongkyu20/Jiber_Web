package com.jiber.backend.notice;

import com.jiber.backend.auth.AuthUserPrincipal;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/notices")
public class AdminNoticeController {

    private final NoticeService noticeService;

    public AdminNoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @GetMapping
    public NoticeListResponse listAdminNotices(@Valid @ParameterObject @ModelAttribute NoticeListRequest request) {
        return noticeService.listAdminNotices(request);
    }

    @GetMapping("/{noticeId}")
    public NoticeDetailResponse getAdminNotice(@PathVariable Long noticeId) {
        return noticeService.getAdminNotice(noticeId);
    }

    @PostMapping
    public NoticeMutationResponse createNotice(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @Valid @RequestBody NoticeUpsertRequest request
    ) {
        return noticeService.createNotice(request, userId(principal));
    }

    @PutMapping("/{noticeId}")
    public NoticeMutationResponse updateNotice(
            @AuthenticationPrincipal AuthUserPrincipal principal,
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpsertRequest request
    ) {
        return noticeService.updateNotice(noticeId, request, userId(principal));
    }

    @DeleteMapping("/{noticeId}")
    public NoticeMutationResponse deleteNotice(@PathVariable Long noticeId) {
        return noticeService.deleteNotice(noticeId);
    }

    private Long userId(AuthUserPrincipal principal) {
        return principal == null ? null : principal.userId();
    }
}
