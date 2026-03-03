package com.comicsai.model.dto;

import com.comicsai.model.enums.ContentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StorylineCreateDTO {

    @NotBlank(message = "故事线标题不能为空")
    private String title;

    @NotBlank(message = "题材类型不能为空")
    private String genre;

    @NotNull(message = "内容类型不能为空")
    private ContentType contentType;

    @NotBlank(message = "角色设定不能为空")
    private String characterSettings;

    @NotBlank(message = "世界观描述不能为空")
    private String worldview;

    @NotBlank(message = "剧情大纲不能为空")
    private String plotOutline;

    public StorylineCreateDTO() {}

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public String getCharacterSettings() { return characterSettings; }
    public void setCharacterSettings(String characterSettings) { this.characterSettings = characterSettings; }

    public String getWorldview() { return worldview; }
    public void setWorldview(String worldview) { this.worldview = worldview; }

    public String getPlotOutline() { return plotOutline; }
    public void setPlotOutline(String plotOutline) { this.plotOutline = plotOutline; }
}
