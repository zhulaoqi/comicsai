package com.comicsai.ai.qwen;

import com.comicsai.ai.ImageAiProvider;
import com.comicsai.ai.ImageGenerationRequest;
import com.comicsai.ai.ImageGenerationResult;
import com.comicsai.common.exception.AiProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

/**
 * Alibaba Wanxiang (通义万相) image generation adapter.
 * DashScope image generation is async: submit task → poll until succeeded → download image bytes.
 */
public class WanxiangImageProvider implements ImageAiProvider {

    private static final Logger log = LoggerFactory.getLogger(WanxiangImageProvider.class);
    private static final String PROVIDER_NAME = "wanxiang";
    private static final String SUBMIT_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text2image/image-synthesis";
    private static final String TASK_URL = "https://dashscope.aliyuncs.com/api/v1/tasks/";
    private static final int MAX_POLL_ATTEMPTS = 30;
    private static final long POLL_INTERVAL_MS = 3000;

    private final String apiKey;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public WanxiangImageProvider(String apiKey, String baseUrl) {
        this.apiKey = apiKey;
    }

    @Override
    public ImageGenerationResult generateImage(ImageGenerationRequest request) {
        if (request == null || request.getPrompt() == null || request.getPrompt().isBlank()) {
            throw new AiProviderException(PROVIDER_NAME, "Prompt must not be null or blank");
        }

        String model = request.getModel() != null ? request.getModel() : "wanx-v1";
        String size = request.getSize() != null ? request.getSize() : "1024x1024";
        log.info("Calling Wanxiang image generation: model={}, size={}", model, size);

        try {
            // Step 1: Submit task
            String taskId = submitTask(request.getPrompt(), model, size);
            log.info("Wanxiang task submitted: taskId={}", taskId);

            // Step 2: Poll until done
            String imageUrl = pollForResult(taskId);
            log.info("Wanxiang task completed, imageUrl={}", imageUrl);

            // Step 3: Download image bytes
            byte[] imageData = downloadImage(imageUrl);
            return new ImageGenerationResult(imageData, "png", 1, model);

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            throw new AiProviderException(PROVIDER_NAME, "Failed to call Wanxiang API: " + e.getMessage(), e);
        }
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
            throw new AiProviderException(PROVIDER_NAME, "Empty response when submitting Wanxiang task");
        }

        String taskId = respBody.path("output").path("task_id").asText(null);
        if (taskId == null || taskId.isBlank()) {
            throw new AiProviderException(PROVIDER_NAME, "No task_id in Wanxiang response: " + respBody);
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
                    throw new AiProviderException(PROVIDER_NAME, "No image URL in Wanxiang result");
                }
                return url;
            } else if ("FAILED".equals(status)) {
                String errMsg = body.path("output").path("message").asText("unknown error");
                throw new AiProviderException(PROVIDER_NAME, "Wanxiang task failed: " + errMsg);
            }
            // PENDING or RUNNING — keep polling
        }
        throw new AiProviderException(PROVIDER_NAME, "Wanxiang task timed out after " + MAX_POLL_ATTEMPTS + " attempts");
    }

    private byte[] downloadImage(String url) {
        ResponseEntity<byte[]> response = restTemplate.getForEntity(url, byte[].class);
        byte[] data = response.getBody();
        if (data == null || data.length == 0) {
            throw new AiProviderException(PROVIDER_NAME, "Downloaded empty image from: " + url);
        }
        return data;
    }

    @Override
    public String getProviderName() {
        return PROVIDER_NAME;
    }
}
