package com.comicsai.model.vo;

import com.comicsai.model.entity.NovelChapter;

import java.math.BigDecimal;

public class NovelChapterVO {

    private Long id;
    private Long contentId;
    private Integer chapterNumber;
    private String chapterTitle;
    private String chapterText;
    private String chapterSummary;
    private BigDecimal price;
    private boolean accessible;
    private BigDecimal chapterPrice;

    public NovelChapterVO() {}

    public static NovelChapterVO fromChapter(NovelChapter ch, boolean accessible, BigDecimal effectivePrice) {
        NovelChapterVO vo = new NovelChapterVO();
        vo.setId(ch.getId());
        vo.setContentId(ch.getContentId());
        vo.setChapterNumber(ch.getChapterNumber());
        vo.setChapterTitle(ch.getChapterTitle());
        vo.setChapterText(accessible ? ch.getChapterText() : null);
        vo.setChapterSummary(ch.getChapterSummary());
        vo.setPrice(ch.getPrice());
        vo.setAccessible(accessible);
        vo.setChapterPrice(effectivePrice);
        return vo;
    }

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

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isAccessible() { return accessible; }
    public void setAccessible(boolean accessible) { this.accessible = accessible; }

    public BigDecimal getChapterPrice() { return chapterPrice; }
    public void setChapterPrice(BigDecimal chapterPrice) { this.chapterPrice = chapterPrice; }
}
