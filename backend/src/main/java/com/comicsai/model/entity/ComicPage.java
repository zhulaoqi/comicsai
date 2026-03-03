package com.comicsai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("comic_page")
public class ComicPage {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("content_id")
    private Long contentId;

    @TableField("page_number")
    private Integer pageNumber;

    @TableField("image_url")
    private String imageUrl;

    @TableField("dialogue_text")
    private String dialogueText;

    public ComicPage() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public Integer getPageNumber() { return pageNumber; }
    public void setPageNumber(Integer pageNumber) { this.pageNumber = pageNumber; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getDialogueText() { return dialogueText; }
    public void setDialogueText(String dialogueText) { this.dialogueText = dialogueText; }
}
