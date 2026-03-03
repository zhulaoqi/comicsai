package com.comicsai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("novel_chapter")
public class NovelChapter {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("content_id")
    private Long contentId;

    @TableField("chapter_number")
    private Integer chapterNumber;

    @TableField("chapter_title")
    private String chapterTitle;

    @TableField("chapter_text")
    private String chapterText;

    @TableField("chapter_summary")
    private String chapterSummary;

    public NovelChapter() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public Integer getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getChapterTitle() { return chapterTitle; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }

    public String getChapterText() { return chapterText; }
    public void setChapterText(String chapterText) { this.chapterText = chapterText; }

    public String getChapterSummary() { return chapterSummary; }
    public void setChapterSummary(String chapterSummary) { this.chapterSummary = chapterSummary; }
}
