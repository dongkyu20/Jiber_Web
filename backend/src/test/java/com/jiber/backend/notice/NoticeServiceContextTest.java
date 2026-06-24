package com.jiber.backend.notice;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

import com.jiber.backend.notice.service.NoticeService;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class NoticeServiceContextTest {

    @Test
    void springCanInstantiateNoticeServiceWithMapperConstructorInjection() {
        assertThatCode(() -> {
            try (var context = new AnnotationConfigApplicationContext()) {
                context.registerBean(NoticeMapper.class, () -> mock(NoticeMapper.class));
                context.registerBean(NoticeService.class);
                context.refresh();
            }
        }).doesNotThrowAnyException();
    }
}
