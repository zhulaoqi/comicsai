package com.comicsai.ai.qwen;

import com.comicsai.ai.TextAiProvider;
import com.comicsai.ai.TextGenerationRequest;
import com.comicsai.ai.TextGenerationResult;
import com.comicsai.common.exception.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

public class QwenTextProvider implements TextAiProvider {

    private static final Logger log = LoggerFactory.getLogger(QwenTextProvider.class);
    private static final String PROVIDER_NAME = "qwen";
    private static final String DEFAULT_MODEL = "qwen-turbo";
    private static final String API_URL = "https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions";

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public QwenTextProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
    }

    @Override
    public TextGenerationResult generateText(TextGenerationRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new AiProviderException(PROVIDER_NAME, "Prompt must not be null or blank");
        }

        String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;
        log.info("Calling Qwen text generation: model={}", model);

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);

            ArrayNode messages = objectMapper.createArrayNode();
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
                messages.addObject().put("role", "system").put("content", request.getSystemPrompt());
            }
            messages.addObject().put("role", "user").put("content", request.getPrompt());
            body.set("messages", messages);

            if (request.getTemperature() != null) {
                body.put("temperature", request.getTemperature());
            }
            if (request.getMaxTokens() != null) {
                body.put("max_tokens", request.getMaxTokens());
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
                throw new AiProviderException(PROVIDER_NAME, "Empty response from Qwen API");
            }

            String content = respBody.path("choices").get(0)
                    .path("message").path("content").asText();

            JsonNode usage = respBody.path("usage");
            int inputTokens = usage.path("prompt_tokens").asInt(0);
            int outputTokens = usage.path("completion_tokens").asInt(0);

            log.info("Qwen response: inputTokens={}, outputTokens={}", inputTokens, outputTokens);
            return new TextGenerationResult(content, inputTokens, outputTokens, model);

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to call Qwen API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
