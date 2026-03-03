package com.comicsai.ai.openai;

import com.comicsai.ai.TextGenerationRequest;
import com.comicsai.common.exception.AiProviderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAiTextProviderTest {

    private final OpenAiTextProvider provider = new OpenAiTextProvider("test-key", "https://api.openai.com");

    @Test
    void getProviderName_returnsOpenai() {
        assertEquals("openai", provider.getProviderName());
    }

    @Test
    void generateText_nullRequest_throws() {
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateText(null));
        assertEquals("openai", ex.getProviderName());
    }

    @Test
    void generateText_nullPrompt_throws() {
        TextGenerationRequest request = new TextGenerationRequest(null, null, 0.7, 100, "gpt-4");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateText(request));
        assertEquals("openai", ex.getProviderName());
    }

    @Test
    void generateText_blankPrompt_throws() {
        TextGenerationRequest request = new TextGenerationRequest("   ", null, 0.7, 100, "gpt-4");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateText(request));
        assertEquals("openai", ex.getProviderName());
    }

    @Test
    void generateText_validRequest_throwsPlaceholder() {
        // The stub implementation throws because there's no real HTTP client.
        // This verifies the adapter validates input before attempting the call.
        TextGenerationRequest request = new TextGenerationRequest("hello", "system", 0.7, 100, "gpt-4");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateText(request));
        assertEquals("openai", ex.getProviderName());
        assertTrue(ex.getMessage().contains("not implemented"));
    }
}
