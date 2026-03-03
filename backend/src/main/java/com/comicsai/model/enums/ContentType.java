package com.comicsai.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

public enum ContentType {
    COMIC("COMIC"),
    NOVEL("NOVEL");

    @EnumValue
    private final String value;

    ContentType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
