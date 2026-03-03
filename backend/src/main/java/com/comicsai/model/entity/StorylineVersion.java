package com.comicsai.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("storyline_version")
public class StorylineVersion {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("storyline_id")
    private Long storylineId;

    @TableField("version_number")
    private Integer versionNumber;

    @TableField("snapshot_json")
    private String snapshotJson;

    @TableField("created_at")
    private LocalDateTime createdAt;

    public StorylineVersion() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getStorylineId() { return storylineId; }
    public void setStorylineId(Long storylineId) { this.storylineId = storylineId; }

    public Integer getVersionNumber() { return versionNumber; }
    public void setVersionNumber(Integer versionNumber) { this.versionNumber = versionNumber; }

    public String getSnapshotJson() { return snapshotJson; }
    public void setSnapshotJson(String snapshotJson) { this.snapshotJson = snapshotJson; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
