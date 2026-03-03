package com.comicsai.ai.qwen;

import com.comicsai.ai.ImageGenerationRequest;
import com.comicsai.common.exception.AiProviderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WanxiangImageProviderTest {

    private final WanxiangImageProvider provider = new WanxiangImageProvider("test-key", "https://dashscope.aliyuncs.com");

    @Test
    void getProviderName_returnsWanxiang() {
        assertEquals("wanxiang", provider.getProviderName());
    }

    @Test
    void generateImage_nullRequest_throws() {
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateImage(null));
        assertEquals("wanxiang", ex.getProviderName());
    }

    @Test
    void generateImage_nullPrompt_throws() {
        ImageGenerationRequest request = new ImageGenerationRequest(null, "anime", "1024x1024", "wanx-v1");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateImage(request));
        assertEquals("wanxiang", ex.getProviderName());
    }

    @Test
    void generateImage_blankPrompt_throws() {
        ImageGenerationRequest request = new ImageGenerationRequest("  ", "anime", "1024x1024", "wanx-v1");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateImage(request));
        assertEquals("wanxiang", ex.getProviderName());
    }

    @Test
    void generateImage_validRequest_throwsPlaceholder() {
        ImageGenerationRequest request = new ImageGenerationRequest("a cat", "anime", "1024x1024", "wanx-v1");
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> provider.generateImage(request));
        assertEquals("wanxiang", ex.getProviderName());
        assertTrue(ex.getMessage().contains("not implemented"));
    }
}
