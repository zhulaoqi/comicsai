package com.comicsai.ai.model.qwen;

import com.comicsai.ai.message.Msg;
import com.comicsai.ai.model.ModelConfig;
import com.comicsai.common.exception.AiProviderException;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class QwenChatModelTest {

    private final QwenChatModel model = new QwenChatModel("test-key");

    @Test
    void getName_returnsQwen() {
        assertEquals("qwen", model.getName());
    }

    @Test
    void chat_nullMessages_throws() {
        ModelConfig config = ModelConfig.builder().modelName("qwen-turbo").build();
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> model.chat(null, config));
        assertEquals("qwen", ex.getProviderName());
    }

    @Test
    void chat_emptyMessages_throws() {
        ModelConfig config = ModelConfig.builder().modelName("qwen-turbo").build();
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> model.chat(Collections.emptyList(), config));
        assertEquals("qwen", ex.getProviderName());
    }

    @Test
    void chat_validMessages_throwsDueToNetwork() {
        List<Msg> messages = List.of(
                Msg.builder().role(Msg.ROLE_USER).content("hello").build()
        );
        ModelConfig config = ModelConfig.builder().modelName("qwen-turbo").build();
        AiProviderException ex = assertThrows(AiProviderException.class,
                () -> model.chat(messages, config));
        assertEquals("qwen", ex.getProviderName());
        assertTrue(ex.getMessage().contains("Failed to call Qwen API"));
    }
}
