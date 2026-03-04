package com.comicsai.model.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.Map;
import java.util.Set;

public enum ContentStatus {
    PENDING_REVIEW("PENDING_REVIEW"),
    PENDING_PUBLISH("PENDING_PUBLISH"),
    PUBLISHED("PUBLISHED"),
    REJECTED("REJECTED"),
    OFFLINE("OFFLINE"),
    REGENERATING("REGENERATING");

    @EnumValue
    private final String value;

    ContentStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    private static final Map<ContentStatus, Set<ContentStatus>> VALID_TRANSITIONS = Map.of(
            PENDING_REVIEW, Set.of(PENDING_PUBLISH, REJECTED, REGENERATING),
            PENDING_PUBLISH, Set.of(PUBLISHED, REGENERATING),
            PUBLISHED, Set.of(OFFLINE),
            OFFLINE, Set.of(PUBLISHED),
            REJECTED, Set.of(REGENERATING),
            REGENERATING, Set.of(PENDING_REVIEW)
    );

    public boolean canTransitionTo(ContentStatus target) {
        Set<ContentStatus> allowed = VALID_TRANSITIONS.get(this);
        return allowed != null && allowed.contains(target);
    }
}
