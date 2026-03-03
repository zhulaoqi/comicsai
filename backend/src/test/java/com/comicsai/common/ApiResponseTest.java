package com.comicsai.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void successWithData() {
        ApiResponse<String> response = ApiResponse.success("hello");
        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertEquals("hello", response.getData());
    }

    @Test
    void successWithoutData() {
        ApiResponse<Void> response = ApiResponse.success();
        assertEquals(200, response.getCode());
        assertEquals("success", response.getMessage());
        assertNull(response.getData());
    }

    @Test
    void errorResponse() {
        ApiResponse<Void> response = ApiResponse.error(404, "not found");
        assertEquals(404, response.getCode());
        assertEquals("not found", response.getMessage());
        assertNull(response.getData());
    }
}
