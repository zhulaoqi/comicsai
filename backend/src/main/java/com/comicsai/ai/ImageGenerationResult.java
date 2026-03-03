package com.comicsai.ai;

public class ImageGenerationResult {

    private byte[] imageData;
    private String format;
    private Integer inputTokens;
    private String model;

    public ImageGenerationResult() {}

    public ImageGenerationResult(byte[] imageData, String format, Integer inputTokens, String model) {
        this.imageData = imageData;
        this.format = format;
        this.inputTokens = inputTokens;
        this.model = model;
    }

    public byte[] getImageData() { return imageData; }
    public void setImageData(byte[] imageData) { this.imageData = imageData; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }

    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
