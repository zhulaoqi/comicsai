package com.comicsai.ai.model.gemini;

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
 * Google Gemini chat model via REST API.
 */
public class GeminiChatModel implements ChatModel {

    private static final Logger log = LoggerFactory.getLogger(GeminiChatModel.class);
    private static final String NAME = "gemini";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiChatModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Msg chat(List<Msg> messages, ModelConfig config) {
        if (messages == null || messages.isEmpty()) {
            throw new AiProviderException(NAME, "Messages must not be empty");
        }

        String model = config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL;
        String url = API_BASE + "/" + model + ":generateContent?key=" + apiKey;
        log.info("Gemini chat: model={}", model);

        try {
            ObjectNode body = objectMapper.createObjectNode();

            // Extract system instruction from messages
            messages.stream()
                    .filter(m -> Msg.ROLE_SYSTEM.equals(m.getRole()))
                    .findFirst()
                    .ifPresent(sysMsg -> {
                        ObjectNode si = objectMapper.createObjectNode();
                        ArrayNode parts = objectMapper.createArrayNode();
                        parts.addObject().put("text", sysMsg.getContent());
                        si.set("parts", parts);
                        body.set("systemInstruction", si);
                    });

            // Build user/assistant turns
            ArrayNode contents = objectMapper.createArrayNode();
            for (Msg m : messages) {
                if (Msg.ROLE_SYSTEM.equals(m.getRole())) continue;
                ObjectNode turn = objectMapper.createObjectNode();
                turn.put("role", Msg.ROLE_ASSISTANT.equals(m.getRole()) ? "model" : "user");
                ArrayNode parts = objectMapper.createArrayNode();
                parts.addObject().put("text", m.getContent());
                turn.set("parts", parts);
                contents.add(turn);
            }
            body.set("contents", contents);

            ObjectNode genConfig = objectMapper.createObjectNode();
            if (config.getTemperature() != null) {
                genConfig.put("temperature", config.getTemperature());
            }
            if (config.getMaxTokens() != null) {
                genConfig.put("maxOutputTokens", config.getMaxTokens());
            }
            body.set("generationConfig", genConfig);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url, HttpMethod.POST,
                    new HttpEntity<>(body.toString(), headers),
                    JsonNode.class);

            JsonNode respBody = response.getBody();
            if (respBody == null) {
                throw new AiProviderException(NAME, "Empty response from Gemini API");
            }

            String content = respBody
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            JsonNode usage = respBody.path("usageMetadata");
            int inputTokens = usage.path("promptTokenCount").asInt(0);
            int outputTokens = usage.path("candidatesTokenCount").asInt(0);

            log.info("Gemini response: inputTokens={}, outputTokens={}", inputTokens, outputTokens);

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
            throw new AiProviderException(NAME, "Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
