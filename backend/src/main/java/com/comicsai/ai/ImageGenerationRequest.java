package com.comicsai.ai;

public class ImageGenerationRequest {

    private String prompt;
    private String style;
    private String size;
    private String model;

    public ImageGenerationRequest() {}

    public ImageGenerationRequest(String prompt, String style, String size, String model) {
        this.prompt = prompt;
        this.style = style;
        this.size = size;
        this.model = model;
    }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getStyle() { return style; }
    public void setStyle(String style) { this.style = style; }

    public String getSize() { return size; }
    public void setSize(String size) { this.size = size; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
}
