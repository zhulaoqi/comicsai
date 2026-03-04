package com.comicsai.ai.config;

import com.comicsai.ai.model.ChatModel;
import com.comicsai.ai.model.ImageModel;
import com.comicsai.ai.model.gemini.GeminiChatModel;
import com.comicsai.ai.model.qwen.QwenChatModel;
import com.comicsai.ai.model.qwen.WanxiangImageModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration that wires AI model beans from AiProperties.
 */
@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfiguration {

    private static final Logger log = LoggerFactory.getLogger(AiConfiguration.class);

    @Bean
    public ChatModel qwenChatModel(AiProperties props) {
        log.info("Registering QwenChatModel");
        return new QwenChatModel(props.getQwen().getApiKey());
    }

    @Bean
    public ChatModel geminiChatModel(AiProperties props) {
        log.info("Registering GeminiChatModel");
        return new GeminiChatModel(props.getGemini().getApiKey());
    }

    @Bean
    public ImageModel wanxiangImageModel(AiProperties props) {
        log.info("Registering WanxiangImageModel");
        return new WanxiangImageModel(props.getQwen().getApiKey());
    }
}
