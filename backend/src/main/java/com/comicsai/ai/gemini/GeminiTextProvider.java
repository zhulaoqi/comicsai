package com.comicsai.ai.gemini;

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

/**
 * Google Gemini text generation adapter.
 * Uses the Gemini generateContent REST API.
 */
public class GeminiTextProvider implements TextAiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiTextProvider.class);
    private static final String PROVIDER_NAME = "gemini";
    private static final String DEFAULT_MODEL = "gemini-1.5-flash";
    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models";

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiTextProvider(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public TextGenerationResult generateText(TextGenerationRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new AiProviderException(PROVIDER_NAME, "Prompt must not be null or blank");
        }

        String model = request.getModel() != null ? request.getModel() : DEFAULT_MODEL;
        String url = API_BASE + "/" + model + ":generateContent?key=" + apiKey;

        log.info("Calling Gemini text generation: model={}", model);

        try {
            ObjectNode body = objectMapper.createObjectNode();

            // System instruction (if provided)
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
                ObjectNode systemInstruction = objectMapper.createObjectNode();
                ArrayNode sysParts = objectMapper.createArrayNode();
                sysParts.addObject().put("text", request.getSystemPrompt());
                systemInstruction.set("parts", sysParts);
                body.set("systemInstruction", systemInstruction);
            }

            // User message
            ArrayNode contents = objectMapper.createArrayNode();
            ObjectNode userContent = objectMapper.createObjectNode();
            userContent.put("role", "user");
            ArrayNode parts = objectMapper.createArrayNode();
            parts.addObject().put("text", request.getPrompt());
            userContent.set("parts", parts);
            contents.add(userContent);
            body.set("contents", contents);

            // Generation config
            ObjectNode genConfig = objectMapper.createObjectNode();
            if (request.getTemperature() != null) {
                genConfig.put("temperature", request.getTemperature());
            }
            if (request.getMaxTokens() != null) {
                genConfig.put("maxOutputTokens", request.getMaxTokens());
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
                throw new AiProviderException(PROVIDER_NAME, "Empty response from Gemini API");
            }

            // Extract text content
            String content = respBody
                    .path("candidates").get(0)
                    .path("content").path("parts").get(0)
                    .path("text").asText();

            // Extract token usage
            JsonNode usage = respBody.path("usageMetadata");
            int inputTokens = usage.path("promptTokenCount").asInt(0);
            int outputTokens = usage.path("candidatesTokenCount").asInt(0);

            log.info("Gemini response: inputTokens={}, outputTokens={}", inputTokens, outputTokens);
            return new TextGenerationResult(content, inputTokens, outputTokens, model);

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to call Gemini API: " + e.getMessage(), e);
        }
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
