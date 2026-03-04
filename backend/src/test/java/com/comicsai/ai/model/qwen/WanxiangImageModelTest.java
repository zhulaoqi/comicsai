package com.comicsai.ai.model.qwen;

import com.comicsai.ai.model.ModelConfig;
import com.comicsai.common.exception.AiProviderException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WanxiangImageModelTest {

    private final WanxiangImageModel model = new WanxiangImageModel("test-key");

    @Test
    void getName_returnsWanxiang() {
        assertEquals("wanxiang", model.getName());
    }

    @Test
    void generateImage_nullPrompt_throws() {
        ModelConfig config = ModelConfig.builder().modelName("wanx-v1").imageSize("1024*1024").build();
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> model.generateImage(null, config));
        assertEquals("wanxiang", ex.getProviderName());
    }

    @Test
    void generateImage_blankPrompt_throws() {
        ModelConfig config = ModelConfig.builder().modelName("wanx-v1").imageSize("1024*1024").build();
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> model.generateImage("  ", config));
        assertEquals("wanxiang", ex.getProviderName());
    }

    // ==================== Size normalization ====================

    @Test
    void normalizeSize_convertsXToAsterisk() {
        assertEquals("1024*1024", WanxiangImageModel.normalizeSize("1024x1024"));
    }

    @Test
    void normalizeSize_convertsUppercaseXToAsterisk() {
        assertEquals("1024*1024", WanxiangImageModel.normalizeSize("1024X1024"));
    }

    @Test
    void normalizeSize_alreadyCorrectFormat() {
        assertEquals("1024*1024", WanxiangImageModel.normalizeSize("1024*1024"));
    }

    @Test
    void normalizeSize_validPortraitSize() {
        assertEquals("720*1280", WanxiangImageModel.normalizeSize("720x1280"));
    }

    @Test
    void normalizeSize_validLandscapeSize() {
        assertEquals("1280*720", WanxiangImageModel.normalizeSize("1280x720"));
    }

    @Test
    void normalizeSize_invalidSize_fallsBackToDefault() {
        assertEquals("1024*1024", WanxiangImageModel.normalizeSize("512x512"));
    }

    @Test
    void normalizeSize_null_fallsBackToDefault() {
        assertEquals("1024*1024", WanxiangImageModel.normalizeSize(null));
    }

    @Test
    void normalizeSize_blank_fallsBackToDefault() {
        assertEquals("1024*1024", WanxiangImageModel.normalizeSize("  "));
    }
}
