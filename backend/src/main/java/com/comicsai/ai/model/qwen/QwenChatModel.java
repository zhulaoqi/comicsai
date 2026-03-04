package com.comicsai.ai.model.qwen;

import com.comicsai.ai.message.Msg;
import com.comicsai.ai.model.ChatModel;
import com.comicsai.ai.model.ModelConfig;
import com.comicsai.common.exception.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Alibaba Qwen (通义千问) chat model via DashScope compatible-mode API.
 */
public class QwenChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(QwenChatModel.class);
    private static final String NAME = "qwen";
    private static final String DEFAULT_MODEL = "qwen-turbo";
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QwenChatModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Msg chat(List<Msg> messages, ModelConfig config) {
        if (messages == null || messages.isEmpty()) {
            throw new AiProviderException(NAME, "Messages must not be empty");
        }

        String model = config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL;
        log.info("Qwen chat: model={}", model);

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);

            ArrayNode msgArray = objectMapper.createArrayNode();
            for (Msg m : messages) {
                msgArray.addObject()
                        .put("role", m.getRole())
                        .put("content", m.getContent());
            }
            body.set("messages", msgArray);

            if (config.getTemperature() != null) {
                body.put("temperature", config.getTemperature());
            }
            if (config.getMaxTokens() != null) {
                body.put("max_tokens", config.getMaxTokens());
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    API_URL, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), headers),
                    JsonNode.class);

            JsonNode respBody = response.getBody();
            if (respBody == null) {
                throw new AiProviderException(NAME, "Empty response from Qwen API");
            }

            String content = respBody.path("choices").get(0)
                    .path("message").path("content").asText();

            JsonNode usage = respBody.path("usage");
            int inputTokens = usage.path("prompt_tokens").asInt(0);
            int outputTokens = usage.path("completion_tokens").asInt(0);

            log.info("Qwen response: inputTokens={}, outputTokens={}", inputTokens, outputTokens);

            return Msg.builder()
                    .name(NAME)
                    .role(Msg.ROLE_ASSISTANT)
                    .content(content)
                    .meta("inputTokens", inputTokens)
                    .meta("outputTokens", outputTokens)
                    .meta("model", model)
                    .build();

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(NAME, "Failed to call Qwen API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
