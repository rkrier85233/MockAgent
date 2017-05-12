package com.cleo.prototype.entities.dataflow;

import java.util.concurrent.TimeUnit;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Recurrence {
    private boolean enabled;
    private Integer interval;
    private TimeUnit timeUnit;

    // Public no arg constructor needed by persistence layer.
    public Recurrence() {
    }

    @Builder
    private Recurrence(Integer interval, TimeUnit timeUnit) {
        this.enabled = true;
        this.interval = interval;
        this.timeUnit = timeUnit;
    }
}
