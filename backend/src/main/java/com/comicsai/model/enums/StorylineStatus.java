package com.comicsai.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum StorylineStatus {
    ENABLED("ENABLED"),
    DISABLED("DISABLED");

    @EnumValue
    private final String value;

    StorylineStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
