package com.comicsai.ai.qwen;

import com.comicsai.ai.TextGenerationRequest;
import com.comicsai.common.exception.AiProviderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QwenTextProviderTest {

    private final QwenTextProvider provider = new QwenTextProvider("test-key", "https://dashscope.aliyuncs.com");

    @Test
    void getProviderName_returnsQwen() {
        assertEquals("qwen", provider.getProviderName());
    }

    @Test
    void generateText_nullRequest_throws() {
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateText(null));
        assertEquals("qwen", ex.getProviderName());
    }

    @Test
    void generateText_nullPrompt_throws() {
        TextGenerationRequest request = new TextGenerationRequest(null, null, 0.7, 100, "qwen-turbo");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateText(request));
        assertEquals("qwen", ex.getProviderName());
    }

    @Test
    void generateText_blankPrompt_throws() {
        TextGenerationRequest request = new TextGenerationRequest("   ", null, 0.7, 100, "qwen-turbo");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateText(request));
        assertEquals("qwen", ex.getProviderName());
    }

    @Test
    void generateText_validRequest_throwsPlaceholder() {
        TextGenerationRequest request = new TextGenerationRequest("hello", "system", 0.7, 100, "qwen-turbo");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateText(request));
        assertEquals("qwen", ex.getProviderName());
        assertTrue(ex.getMessage().contains("not implemented"));
    }
}
