package com.comicsai.model.vo;

import com.comicsai.model.entity.ContentUnlock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class UnlockRecordVO {

    private Long id;
    private Long contentId;
    private BigDecimal pricePaid;
    private LocalDateTime unlockedAt;

    public UnlockRecordVO() {}

    public UnlockRecordVO(Long id, Long contentId, BigDecimal pricePaid, LocalDateTime unlockedAt) {
        this.id = id;
        this.contentId = contentId;
        this.pricePaid = pricePaid;
        this.unlockedAt = unlockedAt;
    }

    public static UnlockRecordVO fromEntity(ContentUnlock unlock) {
        return new UnlockRecordVO(unlock.getId(), unlock.getContentId(),
                unlock.getPricePaid(), unlock.getUnlockedAt());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getContentId() { return contentId; }
    public void setContentId(Long contentId) { this.contentId = contentId; }

    public BigDecimal getPricePaid() { return pricePaid; }
    public void setPricePaid(BigDecimal pricePaid) { this.pricePaid = pricePaid; }

    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}
