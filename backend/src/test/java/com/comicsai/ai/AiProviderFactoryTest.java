package com.comicsai.ai;

import com.comicsai.common.exception.AiProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiProviderFactoryTest {

    @Mock
    private TextAiProvider mockOpenAiText;
    @Mock
    private TextAiProvider mockQwenText;
    @Mock
    private ImageAiProvider mockDallE;
    @Mock
    private ImageAiProvider mockWanxiang;

    private AiProviderFactory factory;

    @BeforeEach
    void setUp() {
        lenient().when(mockOpenAiText.getProviderName()).thenReturn("openai");
        lenient().when(mockQwenText.getProviderName()).thenReturn("qwen");
        lenient().when(mockDallE.getProviderName()).thenReturn("dall-e");
        lenient().when(mockWanxiang.getProviderName()).thenReturn("wanxiang");

        Map<String, TextAiProvider> textProviders = new LinkedHashMap<>();
        textProviders.put("openai", mockOpenAiText);
        textProviders.put("qwen", mockQwenText);

        Map<String, ImageAiProvider> imageProviders = new LinkedHashMap<>();
        imageProviders.put("dall-e", mockDallE);
        imageProviders.put("wanxiang", mockWanxiang);

        factory = new AiProviderFactory(textProviders, imageProviders);
    }

    // ==================== getTextProvider ====================

    @Test
    void getTextProvider_returnsCorrectProvider() {
        assertSame(mockOpenAiText, factory.getTextProvider("openai"));
        assertSame(mockQwenText, factory.getTextProvider("qwen"));
    }

    @Test
    void getTextProvider_throwsForUnknownProvider() {
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> factory.getTextProvider("unknown"));
        assertEquals("unknown", ex.getProviderName());
        assertTrue(ex.getMessage().contains("Unknown text AI provider"));
    }

    // ==================== getImageProvider ====================

    @Test
    void getImageProvider_returnsCorrectProvider() {
        assertSame(mockDallE, factory.getImageProvider("dall-e"));
        assertSame(mockWanxiang, factory.getImageProvider("wanxiang"));
    }

    @Test
    void getImageProvider_throwsForUnknownProvider() {
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> factory.getImageProvider("unknown"));
        assertEquals("unknown", ex.getProviderName());
    }

    // ==================== generateTextWithFallback ====================

    @Test
    void generateTextWithFallback_primarySucceeds() {
        TextGenerationRequest request = new TextGenerationRequest("hello", null, 0.7, 100, "gpt-4");
        TextGenerationResult expected = new TextGenerationResult("response", 10, 20, "gpt-4");
        when(mockOpenAiText.generateText(request)).thenReturn(expected);

        TextGenerationResult result = factory.generateTextWithFallback("openai", request);

        assertSame(expected, result);
        verify(mockOpenAiText).generateText(request);
        verify(mockQwenText, never()).generateText(any());
    }

    @Test
    void generateTextWithFallback_primaryFails_fallbackSucceeds() {
        TextGenerationRequest request = new TextGenerationRequest("hello", null, 0.7, 100, "gpt-4");
        TextGenerationResult expected = new TextGenerationResult("qwen response", 10, 20, "qwen-turbo");

        when(mockOpenAiText.generateText(request)).thenThrow(new AiProviderException("openai", "API error"));
        when(mockQwenText.generateText(request)).thenReturn(expected);

        TextGenerationResult result = factory.generateTextWithFallback("openai", request);

        assertSame(expected, result);
        verify(mockOpenAiText).generateText(request);
        verify(mockQwenText).generateText(request);
    }

    @Test
    void generateTextWithFallback_allProvidersFail() {
        TextGenerationRequest request = new TextGenerationRequest("hello", null, 0.7, 100, "gpt-4");

        when(mockOpenAiText.generateText(request)).thenThrow(new AiProviderException("openai", "API error"));
        when(mockQwenText.generateText(request)).thenThrow(new AiProviderException("qwen", "API error"));

        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> factory.generateTextWithFallback("openai", request));
        assertTrue(ex.getMessage().contains("All text AI providers failed"));
    }

    @Test
    void generateTextWithFallback_unknownPrimary_fallsBackToRegistered() {
        TextGenerationRequest request = new TextGenerationRequest("hello", null, 0.7, 100, "gpt-4");
        TextGenerationResult expected = new TextGenerationResult("response", 10, 20, "gpt-4");
        when(mockOpenAiText.generateText(request)).thenReturn(expected);

        TextGenerationResult result = factory.generateTextWithFallback("nonexistent", request);

        assertSame(expected, result);
    }

    // ==================== generateImageWithFallback ====================

    @Test
    void generateImageWithFallback_primarySucceeds() {
        ImageGenerationRequest request = new ImageGenerationRequest("a cat", "vivid", "1024x1024", "dall-e-3");
        ImageGenerationResult expected = new ImageGenerationResult(new byte[]{1, 2, 3}, "png", 100, "dall-e-3");
        when(mockDallE.generateImage(request)).thenReturn(expected);

        ImageGenerationResult result = factory.generateImageWithFallback("dall-e", request);

        assertSame(expected, result);
        verify(mockDallE).generateImage(request);
        verify(mockWanxiang, never()).generateImage(any());
    }

    @Test
    void generateImageWithFallback_primaryFails_fallbackSucceeds() {
        ImageGenerationRequest request = new ImageGenerationRequest("a cat", "vivid", "1024x1024", "dall-e-3");
        ImageGenerationResult expected = new ImageGenerationResult(new byte[]{4, 5, 6}, "png", 50, "wanx-v1");

        when(mockDallE.generateImage(request)).thenThrow(new AiProviderException("dall-e", "API error"));
        when(mockWanxiang.generateImage(request)).thenReturn(expected);

        ImageGenerationResult result = factory.generateImageWithFallback("dall-e", request);

        assertSame(expected, result);
        verify(mockDallE).generateImage(request);
        verify(mockWanxiang).generateImage(request);
    }

    @Test
    void generateImageWithFallback_allProvidersFail() {
        ImageGenerationRequest request = new ImageGenerationRequest("a cat", "vivid", "1024x1024", "dall-e-3");

        when(mockDallE.generateImage(request)).thenThrow(new AiProviderException("dall-e", "API error"));
        when(mockWanxiang.generateImage(request)).thenThrow(new AiProviderException("wanxiang", "API error"));

        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> factory.generateImageWithFallback("dall-e", request));
        assertTrue(ex.getMessage().contains("All image AI providers failed"));
    }

    @Test
    void generateImageWithFallback_unknownPrimary_fallsBackToRegistered() {
        ImageGenerationRequest request = new ImageGenerationRequest("a cat", "vivid", "1024x1024", "dall-e-3");
        ImageGenerationResult expected = new ImageGenerationResult(new byte[]{1}, "png", 100, "dall-e-3");
        when(mockDallE.generateImage(request)).thenReturn(expected);

        ImageGenerationResult result = factory.generateImageWithFallback("nonexistent", request);

        assertSame(expected, result);
    }
}
