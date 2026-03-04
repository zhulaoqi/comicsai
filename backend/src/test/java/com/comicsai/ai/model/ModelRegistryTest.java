package com.comicsai.ai.model;

import com.comicsai.ai.message.Msg;
import com.comicsai.common.exception.AiProviderException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModelRegistryTest {

    @Mock private ChatModel mockQwen;
    @Mock private ChatModel mockGemini;
    @Mock private ImageModel mockWanxiang;

    private ModelRegistry registry;

    @BeforeEach
    void setUp() {
        lenient().when(mockQwen.getName()).thenReturn("qwen");
        lenient().when(mockGemini.getName()).thenReturn("gemini");
        lenient().when(mockWanxiang.getName()).thenReturn("wanxiang");

        Map<String, ChatModel> chatModels = new LinkedHashMap<>();
        chatModels.put("qwen", mockQwen);
        chatModels.put("gemini", mockGemini);

        Map<String, ImageModel> imageModels = new LinkedHashMap<>();
        imageModels.put("wanxiang", mockWanxiang);

        registry = new ModelRegistry(chatModels, imageModels);
    }

    // ==================== getChatModel ====================

    @Test
    void getChatModel_returnsCorrectModel() {
        assertSame(mockQwen, registry.getChatModel("qwen"));
        assertSame(mockGemini, registry.getChatModel("gemini"));
    }

    @Test
    void getChatModel_throwsForUnknown() {
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> registry.getChatModel("unknown"));
        assertEquals("unknown", ex.getProviderName());
    }

    // ==================== getImageModel ====================

    @Test
    void getImageModel_returnsCorrectModel() {
        assertSame(mockWanxiang, registry.getImageModel("wanxiang"));
    }

    @Test
    void getImageModel_throwsForUnknown() {
        assertThrows(AiProviderException.class, () -> registry.getImageModel("unknown"));
    }

    // ==================== chatWithFallback ====================

    @Test
    void chatWithFallback_primarySucceeds() {
        Msg expected = Msg.builder().content("response").build();
        when(mockQwen.chat(any(), any())).thenReturn(expected);

        List<Msg> messages = List.of(Msg.builder().role(Msg.ROLE_USER).content("hello").build());
        ModelConfig config = ModelConfig.builder().modelName("qwen-turbo").build();

        Msg result = registry.chatWithFallback("qwen", messages, config);

        assertSame(expected, result);
        verify(mockQwen).chat(any(), any());
        verify(mockGemini, never()).chat(any(), any());
    }

    @Test
    void chatWithFallback_primaryFails_fallbackSucceeds() {
        Msg expected = Msg.builder().content("gemini response").build();
        when(mockQwen.chat(any(), any())).thenThrow(new AiProviderException("qwen", "error"));
        when(mockGemini.chat(any(), any())).thenReturn(expected);

        List<Msg> messages = List.of(Msg.builder().role(Msg.ROLE_USER).content("hello").build());
        ModelConfig config = ModelConfig.builder().build();

        Msg result = registry.chatWithFallback("qwen", messages, config);

        assertSame(expected, result);
    }

    @Test
    void chatWithFallback_allFail() {
        when(mockQwen.chat(any(), any())).thenThrow(new AiProviderException("qwen", "error1"));
        when(mockGemini.chat(any(), any())).thenThrow(new AiProviderException("gemini", "error2"));

        List<Msg> messages = List.of(Msg.builder().role(Msg.ROLE_USER).content("hello").build());
        ModelConfig config = ModelConfig.builder().build();

        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> registry.chatWithFallback("qwen", messages, config));
        assertTrue(ex.getMessage().contains("All chat models failed"));
    }

    @Test
    void chatWithFallback_unknownPrimary_fallsBackToRegistered() {
        Msg expected = Msg.builder().content("response").build();
        when(mockQwen.chat(any(), any())).thenReturn(expected);

        List<Msg> messages = List.of(Msg.builder().role(Msg.ROLE_USER).content("hello").build());
        ModelConfig config = ModelConfig.builder().build();

        Msg result = registry.chatWithFallback("nonexistent", messages, config);
        assertSame(expected, result);
    }

    // ==================== imageWithFallback ====================

    @Test
    void imageWithFallback_primarySucceeds() {
        Msg expected = Msg.builder().imageData(new byte[]{1, 2, 3}).imageFormat("png").build();
        when(mockWanxiang.generateImage(anyString(), any())).thenReturn(expected);

        ModelConfig config = ModelConfig.builder().imageSize("1024*1024").build();
        Msg result = registry.imageWithFallback("wanxiang", "a cat", config);

        assertSame(expected, result);
    }

    @Test
    void imageWithFallback_allFail() {
        when(mockWanxiang.generateImage(anyString(), any()))
                .thenThrow(new AiProviderException("wanxiang", "error"));

        ModelConfig config = ModelConfig.builder().build();
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> registry.imageWithFallback("wanxiang", "a cat", config));
        assertTrue(ex.getMessage().contains("All image models failed"));
    }
}
