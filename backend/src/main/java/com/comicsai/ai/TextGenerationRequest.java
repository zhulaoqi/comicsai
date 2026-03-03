package com.comicsai.ai;

public class TextGenerationRequest {

    private String prompt;
    private String systemPrompt;
    private Double temperature;
    private Integer maxTokens;
    private String model;

    public TextGenerationRequest() {}

    public TextGenerationRequest(String prompt, String systemPrompt, Double temperature, Integer maxTokens, String model) {
        this.prompt = prompt;
        this.systemPrompt = systemPrompt;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.model = model;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getSystemPrompt() { return systemPrompt; }
    public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
