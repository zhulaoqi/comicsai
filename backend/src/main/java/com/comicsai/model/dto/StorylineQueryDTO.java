package com.comicsai.model.dto;

import com.comicsai.model.enums.ContentType;
import com.comicsai.model.enums.StorylineStatus;

public class StorylineQueryDTO {

    private Integer page = 1;
    private Integer size = 10;
    private ContentType contentType;
    private StorylineStatus status;
    private String genre;

    public StorylineQueryDTO() {}

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public StorylineStatus getStatus() { return status; }
    public void setStatus(StorylineStatus status) { this.status = status; }

    public String getGenre() { return genre; }
    public void setGenre(String genre) { this.genre = genre; }
}
