package com.comicsai.ai;

public class TextGenerationResult {

    private String content;
    private Integer inputTokens;
    private Integer outputTokens;
    private String model;

    public TextGenerationResult() {}

    public TextGenerationResult(String content, Integer inputTokens, Integer outputTokens, String model) {
        this.content = content;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.model = model;
    }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
