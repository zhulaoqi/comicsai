package com.comicsai.common.exception;

import com.comicsai.common.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException() {
        BusinessException ex = new BusinessException(400, "业务错误");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiResponse<Void> apiResponse = handler.handleBusinessException(ex, response);
        assertEquals(400, apiResponse.getCode());
        assertEquals("业务错误", apiResponse.getMessage());
        assertEquals(400, response.getStatus());
    }

    @Test
    void handleBusinessException_409() {
        BusinessException ex = new BusinessException(409, "该邮箱已注册");
        MockHttpServletResponse response = new MockHttpServletResponse();
        ApiResponse<Void> apiResponse = handler.handleBusinessException(ex, response);
        assertEquals(409, apiResponse.getCode());
        assertEquals(409, response.getStatus());
    }

    @Test
    void handleAuthenticationException() {
        AuthenticationException ex = new AuthenticationException();
        ApiResponse<Void> response = handler.handleAuthenticationException(ex);
        assertEquals(401, response.getCode());
    }

    @Test
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("无权限");
        ApiResponse<Void> response = handler.handleAccessDeniedException(ex);
        assertEquals(403, response.getCode());
        assertEquals("无权限", response.getMessage());
    }

    @Test
    void handleEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Content", 1L);
        ApiResponse<Void> response = handler.handleEntityNotFoundException(ex);
        assertEquals(404, response.getCode());
        assertTrue(response.getMessage().contains("Content"));
    }

    @Test
    void handleIllegalStateTransitionException() {
        IllegalStateTransitionException ex = new IllegalStateTransitionException("PENDING_REVIEW", "PUBLISHED");
        ApiResponse<Void> response = handler.handleIllegalStateTransitionException(ex);
        assertEquals(409, response.getCode());
        assertTrue(response.getMessage().contains("PENDING_REVIEW"));
    }

    @Test
    void handleAiProviderException() {
        AiProviderException ex = new AiProviderException("OpenAI", "API调用失败");
        ApiResponse<Void> response = handler.handleAiProviderException(ex);
        assertEquals(503, response.getCode());
    }

    @Test
    void handleGenericException() {
        Exception ex = new RuntimeException("unexpected");
        ApiResponse<Void> response = handler.handleGenericException(ex);
        assertEquals(500, response.getCode());
        assertEquals("服务器内部错误", response.getMessage());
    }
}
