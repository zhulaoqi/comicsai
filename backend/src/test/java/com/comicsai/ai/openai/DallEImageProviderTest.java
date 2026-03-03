package com.comicsai.ai.openai;

import com.comicsai.ai.ImageGenerationRequest;
import com.comicsai.common.exception.AiProviderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DallEImageProviderTest {

    private final DallEImageProvider provider = new DallEImageProvider("test-key", "https://api.openai.com");

    @Test
    void getProviderName_returnsDallE() {
        assertEquals("dall-e", provider.getProviderName());
    }

    @Test
    void generateImage_nullRequest_throws() {
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateImage(null));
        assertEquals("dall-e", ex.getProviderName());
    }

    @Test
    void generateImage_nullPrompt_throws() {
        ImageGenerationRequest request = new ImageGenerationRequest(null, "vivid", "1024x1024", "dall-e-3");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateImage(request));
        assertEquals("dall-e", ex.getProviderName());
    }

    @Test
    void generateImage_blankPrompt_throws() {
        ImageGenerationRequest request = new ImageGenerationRequest("  ", "vivid", "1024x1024", "dall-e-3");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateImage(request));
        assertEquals("dall-e", ex.getProviderName());
    }

    @Test
    void generateImage_validRequest_throwsPlaceholder() {
        ImageGenerationRequest request = new ImageGenerationRequest("a cat", "vivid", "1024x1024", "dall-e-3");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateImage(request));
        assertEquals("dall-e", ex.getProviderName());
        assertTrue(ex.getMessage().contains("not implemented"));
    }
}
