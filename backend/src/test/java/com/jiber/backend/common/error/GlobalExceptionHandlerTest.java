package com.jiber.backend.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

class GlobalExceptionHandlerTest {

    @Test
    void noResourceFoundReturnsNotFoundInsteadOfInternalError() {
        var handler = new GlobalExceptionHandler();
        var request = new MockHttpServletRequest("GET", "/api/v1/news");

        var response = handler.handleNoResourceFound(
                new NoResourceFoundException(HttpMethod.GET, "/api/v1/news"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("RESOURCE_NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("요청한 API를 찾을 수 없습니다.");
    }
}
