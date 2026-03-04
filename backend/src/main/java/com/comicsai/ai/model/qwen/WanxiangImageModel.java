package com.comicsai.ai.model.qwen;

import com.comicsai.ai.message.Msg;
import com.comicsai.ai.model.ImageModel;
import com.comicsai.ai.model.ModelConfig;
import com.comicsai.common.exception.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Set;

/**
 * Alibaba Wanxiang (通义万相) image generation model.
 * DashScope async workflow: submit task -> poll until SUCCEEDED -> download image bytes.
 */
public class WanxiangImageModel implements ImageModel {

    private static final Logger log = LoggerFactory.getLogger(WanxiangImageModel.class);
    private static final String NAME = "wanxiang";
    private static final String DEFAULT_MODEL = "wanx-v1";
    private static final String DEFAULT_SIZE = "1024*1024";
    private static final String SUBMIT_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";
    private static final String TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/";
    private static final int MAX_POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MS = 3000;

    private static final Set<String> VALID_SIZES = Set.of("1024*1024", "720*1280", "1280*720");

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WanxiangImageModel(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public Msg generateImage(String prompt, ModelConfig config) {
        if (prompt == null || prompt.isBlank()) {
            throw new AiProviderException(NAME, "Prompt must not be null or blank");
        }

        String model = config.getModelName() != null ? config.getModelName() : DEFAULT_MODEL;
        String size = normalizeSize(config.getImageSize());
        log.info("Wanxiang image generation: model={}, size={}", model, size);

        try {
            String taskId = submitTask(prompt, model, size);
            log.info("Wanxiang task submitted: taskId={}", taskId);

            String imageUrl = pollForResult(taskId);
            log.info("Wanxiang task completed, imageUrl={}", imageUrl);

            byte[] imageData = downloadImage(imageUrl);

            return Msg.builder()
                    .name(NAME)
                    .role(Msg.ROLE_ASSISTANT)
                    .imageData(imageData)
                    .imageFormat("png")
                    .meta("inputTokens", 1)
                    .meta("model", model)
                    .build();

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(NAME, "Failed to call Wanxiang API: " + e.getMessage(), e);
        }
    }

    /**
     * Normalize size string: converts 'x' separator to '*' as required by DashScope,
     * and validates against the allowed set.
     */
    static String normalizeSize(String size) {
        if (size == null || size.isBlank()) {
            return DEFAULT_SIZE;
        }
        String normalized = size.replace('x', '*').replace('X', '*');
        if (!VALID_SIZES.contains(normalized)) {
            log.warn("Invalid Wanxiang image size '{}', falling back to {}", size, DEFAULT_SIZE);
            return DEFAULT_SIZE;
        }
        return normalized;
    }

    private String submitTask(String prompt, String model, String size) {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("prompt", prompt);

        ObjectNode parameters = objectMapper.createObjectNode();
        parameters.put("size", size);
        parameters.put("n", 1);

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.set("input", input);
        body.set("parameters", parameters);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        headers.set("X-DashScope-Async", "enable");

        ResponseEntity<JsonNode> response = restTemplate.exchange(
                SUBMIT_URL, HttpMethod.POST,
                new HttpEntity<>(body.toString(), headers),
                JsonNode.class);

        JsonNode respBody = response.getBody();
        if (respBody == null) {
            throw new AiProviderException(NAME, "Empty response when submitting Wanxiang task");
        }

        String taskId = respBody.path("output").path("task_id").asText(null);
        if (taskId == null || taskId.isBlank()) {
            throw new AiProviderException(NAME, "No task_id in Wanxiang response: " + respBody);
        }
        return taskId;
    }

    private String pollForResult(String taskId) throws InterruptedException {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (int i = 0; i < MAX_POLL_ATTEMPTS; i++) {
            Thread.sleep(POLL_INTERVAL_MS);

            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    TASK_URL + taskId, HttpMethod.GET, entity, JsonNode.class);

            JsonNode body = response.getBody();
            if (body == null) continue;

            String status = body.path("output").path("task_status").asText("");
            log.debug("Wanxiang task {} status: {}", taskId, status);

            if ("SUCCEEDED".equals(status)) {
                String url = body.path("output").path("results").get(0).path("url").asText(null);
                if (url == null || url.isBlank()) {
                    throw new AiProviderException(NAME, "No image URL in Wanxiang result");
                }
                return url;
            } else if ("FAILED".equals(status)) {
                String errMsg = body.path("output").path("message").asText("unknown error");
                throw new AiProviderException(NAME, "Wanxiang task failed: " + errMsg);
            }
        }
        throw new AiProviderException(NAME, "Wanxiang task timed out after " + MAX_POLL_ATTEMPTS + " attempts");
    }

    private byte[] downloadImage(String url) {
        // Use URI.create() to prevent RestTemplate from re-encoding the presigned URL
        ResponseEntity<byte[]> response = restTemplate.getForEntity(java.net.URI.create(url), byte[].class);
        byte[] data = response.getBody();
        if (data == null || data.length == 0) {
            throw new AiProviderException(NAME, "Downloaded empty image from: " + url);
        }
        return data;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
