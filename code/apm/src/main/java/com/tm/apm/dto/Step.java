package com.tm.apm.dto;

import io.swagger.annotations.ApiModel;

/**
 * @author tangming
 * @date 2022/6/16
 */
@ApiModel
public enum Step {
    DAY(86400000L),
    HOUR(3600000L),
    MINUTE(60000L),
    SECOND(1000L);

    private Long stepTime;

    private Step(long stepTime) {
        this.stepTime = stepTime;
    }

    public Long getStepTime() {
        return this.stepTime;
    }
}
