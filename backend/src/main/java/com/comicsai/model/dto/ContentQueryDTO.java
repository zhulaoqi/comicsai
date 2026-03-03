package com.comicsai.model.dto;

import com.comicsai.model.enums.ContentStatus;
import com.comicsai.model.enums.ContentType;

public class ContentQueryDTO {

    private Integer page = 1;
    private Integer size = 10;
    private ContentType contentType;
    private ContentStatus status;
    private Long storylineId;
    private Boolean isPaid;

    public ContentQueryDTO() {}

    public Integer getPage() { return page; }
    public void setPage(Integer page) { this.page = page; }

    public Integer getSize() { return size; }
    public void setSize(Integer size) { this.size = size; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public ContentStatus getStatus() { return status; }
    public void setStatus(ContentStatus status) { this.status = status; }

    public Long getStorylineId() { return storylineId; }
    public void setStorylineId(Long storylineId) { this.storylineId = storylineId; }

    public Boolean getIsPaid() { return isPaid; }
    public void setIsPaid(Boolean isPaid) { this.isPaid = isPaid; }
}
