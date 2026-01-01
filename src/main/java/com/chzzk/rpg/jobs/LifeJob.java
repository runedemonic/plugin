package com.chzzk.rpg.jobs;

import lombok.Getter;

@Getter
public enum LifeJob {
    NONE("무직"),
    BLACKSMITH("대장장이"),
    CHEF("요리사"),
    BUILDER("건축가");

    private final String displayName;

    LifeJob(String displayName) {
        this.displayName = displayName;
    }
}
